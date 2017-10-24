package fi.oph.koski.cache

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.{MILLISECONDS, _}
import collection.mutable.{Map => MutableMap}
import concurrent.duration._

import com.google.common.cache.AbstractCache.SimpleStatsCounter
import com.google.common.cache.CacheStats
import fi.oph.koski.db.GlobalExecutionContext
import fi.oph.koski.log.Logging
import fi.oph.koski.util.{Futures, Invocation}

import scala.concurrent.Future

/**
  * RefreshingCache caches results of Invocations, keeping a configured number of most recently used keys. The cached
  * values are refreshed on the background so that subsequent requests will get a relatively fresh value from cache.
  *
  * Use RefreshingCache.Params to configure the details:
  *
  * - maximum duration
  * - maximum size (number of items)
  * - maxExcessRatio (default 0.1): how many excess items can be kept in the cache before actually cleaning up (it's not optimal to clean up too often)
  * - refreshScatteringRation (default 0.1): controls how much randomization will be applied to refresh intervals, to prevent huge peak loads on the services behind the cache.
  */
object RefreshingCache {
  def apply(name: String, duration: Duration, maxSize: Int)(implicit manager: CacheManager): RefreshingCache = new RefreshingCache(name, Params(duration, maxSize))

  case class Params(duration: Duration, maxSize: Int, maxExcessRatio: Double = 0.1, refreshScatteringRatio: Double = 0.1) extends CacheParams

  private val refreshExecutor = Executors.newSingleThreadScheduledExecutor
}

class RefreshingCache(val name: String, val params: RefreshingCache.Params)(implicit invalidator: CacheManager) extends Cache with Logging with GlobalExecutionContext {
  private val statsCounter = new SimpleStatsCounter()
  private val maxExcess = (params.maxSize * params.maxExcessRatio).toInt
  private val entries: MutableMap[Invocation, CacheEntry] = MutableMap.empty
  logger.debug("Create refreshing cache " + name)
  invalidator.registerCache(this)

  override def stats: CacheStats = statsCounter.snapshot()

  override def apply(invocation: Invocation): AnyRef = Futures.await(callAsync(invocation), 1 day)

  def callAsync(invocation: Invocation): Future[AnyRef] = synchronized {
    val current = entries.getOrElseUpdate(invocation, new CacheEntry(invocation))
    cleanup
    current.valueFuture
  }

  override def invalidateCache(): Unit = synchronized {
    entries.values.foreach(_.evict)
    entries.clear
  }

  protected[cache] def getEntry(invocation: Invocation) = synchronized(entries.get(invocation))

  private def cleanup = {
    val diff = entries.size - params.maxSize
    if (diff > maxExcess) {
      entries.values.toList.sortBy(_.lastReadTimestamp).take(diff).foreach { entry =>
        entry.evict
        entries.remove(entry.invocation)
      }
    }
  }

  class CacheEntry(protected[cache] val invocation: Invocation) {
    private var lastRead: Long = System.currentTimeMillis
    private var scheduledRefreshTime: Option[Long] = None
    private var currentValue: Option[Future[AnyRef]] = None
    private var cancelled = false
    private var fetcher: Option[Future[AnyRef]] = None

    newFetcher
    scheduleRefresh

    def valueFuture = synchronized {
      lastRead = System.currentTimeMillis
      currentValue match {
        case Some(value) =>
          //logger.info("HIT  " + name + " - " + invocation.toString)
          statsCounter.recordHits(1)
          value
        case None =>
          //logger.info("MISS " + name + " - " + invocation.toString)
          statsCounter.recordMisses(1)
          fetcher.getOrElse(newFetcher)
      }
    }

    def lastReadTimestamp = synchronized(lastRead)

    def evict = synchronized {
      cancelled = true
      statsCounter.recordEviction
    }

    def getScheduledRefreshTime = synchronized(scheduledRefreshTime)

    private def newFetcher: Future[AnyRef] = {
      val start = System.nanoTime()

      val newFetcherFuture = Future {
        try {
          val newValue = invocation.invoke
          statsCounter.recordLoadSuccess(System.nanoTime() - start)
          CacheEntry.this.synchronized {
            currentValue = Some(Future(newValue))
          }
          logger.debug(s"Stored value $newValue for $invocation")
          newValue
        } catch {
          case e: Exception =>
            logger.debug(s"Fetch failed for $invocation")
            statsCounter.recordLoadException(System.nanoTime() - start)
            throw e
        }
      }

      synchronized(fetcher = Some(newFetcherFuture))

      newFetcherFuture.andThen { case _ =>
        CacheEntry.this.synchronized {
          if (Some(newFetcherFuture) == fetcher) {
            // Remove fetcher once done
            fetcher = None
          }
        }
        scheduleRefresh
      }
      newFetcherFuture
    }

    private def scheduleRefresh = synchronized {
      if (!scheduledRefreshTime.isDefined) {
        val variation = params.refreshScatteringRatio // add some random variation to refresh time
        val randomizedFactor: Double = Math.random() * variation + (1.0 - variation)
        val delayMillis = (params.duration.toMillis * randomizedFactor).toLong
        logger.debug(s"Scheduling new fetch for $invocation in " + delayMillis)
        scheduledRefreshTime = Some(System.currentTimeMillis() + delayMillis)
        RefreshingCache.refreshExecutor.schedule(new Runnable { override def run(): Unit = startScheduledRefresh }, delayMillis, MILLISECONDS)
      }
    }

    private def startScheduledRefresh = synchronized {
      scheduledRefreshTime = None
      if (fetcher.isEmpty && !cancelled) {
        logger.debug(s"Starting scheduled refresh for $invocation")
        newFetcher
      }
    }
  }
}