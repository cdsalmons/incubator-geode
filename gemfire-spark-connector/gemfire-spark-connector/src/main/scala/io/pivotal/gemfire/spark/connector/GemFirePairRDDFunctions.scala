package io.pivotal.gemfire.spark.connector

import io.pivotal.gemfire.spark.connector.internal.rdd.{GemFireOuterJoinRDD, GemFireJoinRDD, GemFirePairRDDWriter}
import org.apache.spark.Logging
import org.apache.spark.api.java.function.Function
import org.apache.spark.rdd.RDD

/**
 * Extra gemFire functions on RDDs of (key, value) pairs through an implicit conversion.
 * Import `io.pivotal.gemfire.spark.connector._` at the top of your program to
 * use these functions.
 */
class GemFirePairRDDFunctions[K, V](val rdd: RDD[(K, V)]) extends Serializable with Logging {

  /**
   * Save the RDD of pairs to GemFire key-value store without any conversion
   * @param regionPath the full path of region that the RDD is stored
   * @param connConf the GemFireConnectionConf object that provides connection to GemFire cluster
   */
  def saveToGemfire(regionPath: String, connConf: GemFireConnectionConf = defaultConnectionConf): Unit = {
    connConf.getConnection.validateRegion[K, V](regionPath)
    logInfo(s"Save RDD id=${rdd.id} to region $regionPath")
    val writer = new GemFirePairRDDWriter[K, V](regionPath, connConf)
    rdd.sparkContext.runJob(rdd, writer.write _)
  }

  /**
   * Return an RDD containing all pairs of elements with matching keys in `this`
   * RDD and the GemFire `Region[K, V2]`. Each pair of elements will be returned
   * as a ((k, v), v2) tuple, where (k, v) is in `this` RDD and (k, v2) is in the
   * GemFire region.
   *
   *@param regionPath the region path of the GemFire region
   * @param connConf the GemFireConnectionConf object that provides connection to GemFire cluster
   * @tparam K2 the key type of the GemFire region
   * @tparam V2 the value type of the GemFire region
   * @return RDD[T, V]
   */
  def joinGemfireRegion[K2 <: K, V2](
    regionPath: String, connConf: GemFireConnectionConf = defaultConnectionConf): GemFireJoinRDD[(K, V), K, V2] = {
    new GemFireJoinRDD[(K, V), K, V2](rdd, null, regionPath, connConf)
  }

  /**
   * Return an RDD containing all pairs of elements with matching keys in `this` RDD
   * and the GemFire `Region[K2, V2]`. The join key from RDD element is generated by
   * `func(K, V) => K2`, and the key from the GemFire region is jus the key of the
   * key/value pair.
   *
   * Each pair of elements of result RDD will be returned as a ((k, v), v2) tuple,
   * where (k, v) is in `this` RDD and (k2, v2) is in the GemFire region.
   *
   * @param regionPath the region path of the GemFire region
   * @param func the function that generates region key from RDD element (K, V)
   * @param connConf the GemFireConnectionConf object that provides connection to GemFire cluster
   * @tparam K2 the key type of the GemFire region
   * @tparam V2 the value type of the GemFire region
   * @return RDD[(K, V), V2]
   */
  def joinGemfireRegion[K2, V2](
    regionPath: String, func: ((K, V)) => K2, connConf: GemFireConnectionConf = defaultConnectionConf): GemFireJoinRDD[(K, V), K2, V2] =
    new GemFireJoinRDD[(K, V), K2, V2](rdd, func, regionPath, connConf)

  /** This version of joinGemfireRegion(...) is just for Java API. */
  private[connector] def joinGemfireRegion[K2, V2](
    regionPath: String, func: Function[(K, V), K2], connConf: GemFireConnectionConf): GemFireJoinRDD[(K, V), K2, V2] = {
    new GemFireJoinRDD[(K, V), K2, V2](rdd, func.call, regionPath, connConf)
  }

  /**
   * Perform a left outer join of `this` RDD and the GemFire `Region[K, V2]`.
   * For each element (k, v) in `this` RDD, the resulting RDD will either contain
   * all pairs ((k, v), Some(v2)) for v2 in the GemFire region, or the pair
   * ((k, v), None)) if no element in the GemFire region have key k.
   *
   * @param regionPath the region path of the GemFire region
   * @param connConf the GemFireConnectionConf object that provides connection to GemFire cluster
   * @tparam K2 the key type of the GemFire region
   * @tparam V2 the value type of the GemFire region
   * @return RDD[ (K, V), Option[V] ]
   */
  def outerJoinGemfireRegion[K2 <: K, V2](
    regionPath: String, connConf: GemFireConnectionConf = defaultConnectionConf): GemFireOuterJoinRDD[(K, V), K, V2] = {
    new GemFireOuterJoinRDD[(K, V), K, V2](rdd, null, regionPath, connConf)
  }

  /**
   * Perform a left outer join of `this` RDD and the GemFire `Region[K2, V2]`.
   * The join key from RDD element is generated by `func(K, V) => K2`, and the
   * key from region is jus the key of the key/value pair.
   *
   * For each element (k, v) in `this` RDD, the resulting RDD will either contain
   * all pairs ((k, v), Some(v2)) for v2 in the GemFire region, or the pair
   * ((k, v), None)) if no element in the GemFire region have key `func(k, v)`.
   *
   *@param regionPath the region path of the GemFire region
   * @param func the function that generates region key from RDD element (K, V)
   * @param connConf the GemFireConnectionConf object that provides connection to GemFire cluster
   * @tparam K2 the key type of the GemFire region
   * @tparam V2 the value type of the GemFire region
   * @return RDD[ (K, V), Option[V] ]
   */
  def outerJoinGemfireRegion[K2, V2](
    regionPath: String, func: ((K, V)) => K2, connConf: GemFireConnectionConf = defaultConnectionConf): GemFireOuterJoinRDD[(K, V), K2, V2] = {
    new GemFireOuterJoinRDD[(K, V), K2, V2](rdd, func, regionPath, connConf)
  }

  /** This version of outerJoinGemfireRegion(...) is just for Java API. */
  private[connector] def outerJoinGemfireRegion[K2, V2](
    regionPath: String, func: Function[(K, V), K2], connConf: GemFireConnectionConf): GemFireOuterJoinRDD[(K, V), K2, V2] = {
    new GemFireOuterJoinRDD[(K, V), K2, V2](rdd, func.call, regionPath, connConf)
  }

  private[connector] def defaultConnectionConf: GemFireConnectionConf =
    GemFireConnectionConf(rdd.sparkContext.getConf)

}
