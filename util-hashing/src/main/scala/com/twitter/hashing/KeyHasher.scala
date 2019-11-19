package com.twitter.hashing

/**
 * Hashes a key into a 32-bit or 64-bit number (depending on the algorithm).
 *
 * @see the companion object for common implementations.
 */
trait KeyHasher {
  def hashKey(key: Array[Byte]): Long
}

/**
 * Commonly used key hashing algorithms.
 *
 * @see [[KeyHashers]] for Java compatible forwarders.
 */
object KeyHasher {
  def fromHashableInt(hashable: Hashable[Array[Byte], Int]): KeyHasher = new KeyHasher {
    def hashKey(key: Array[Byte]) = hashable(key).toLong
    override def toString = hashable.toString
  }
  def fromHashableLong(hashable: Hashable[Array[Byte], Long]): KeyHasher = new KeyHasher {
    def hashKey(key: Array[Byte]) = hashable(key)
    override def toString = hashable.toString
  }

  val FNV1_32: KeyHasher = fromHashableInt(Hashable.FNV1_32)
  val FNV1A_32: KeyHasher = fromHashableInt(Hashable.FNV1A_32)
  val FNV1_64: KeyHasher = fromHashableLong(Hashable.FNV1_64)
  val FNV1A_64: KeyHasher = fromHashableLong(Hashable.FNV1A_64)

  /**
   * Ketama's default hash algorithm: the first 4 bytes of the MD5 as a little-endian int.
   */
  val KETAMA: KeyHasher = fromHashableInt(Hashable.MD5_LEInt)
  val CRC32_ITU: KeyHasher = fromHashableInt(Hashable.CRC32_ITU)
  val HSIEH: KeyHasher = fromHashableInt(Hashable.HSIEH)
  val JENKINS: KeyHasher = fromHashableLong(Hashable.JENKINS)
  val MURMUR3: KeyHasher = fromHashableInt(Hashable.MURMUR3)

  /**
   * Return one of the key hashing algorithms by name. This is used to configure a memcache
   * client from a config file.
   */
  def byName(name: String): KeyHasher = {
    name match {
      case "fnv" => FNV1_32
      case "fnv1" => FNV1_32
      case "fnv1-32" => FNV1_32
      case "fnv1a-32" => FNV1A_32
      case "fnv1-64" => FNV1_64
      case "fnv1a-64" => FNV1A_64
      case "ketama" => KETAMA
      case "crc32-itu" => CRC32_ITU
      case "hsieh" => HSIEH
      case "murmur3" => MURMUR3
      case _ => throw new NoSuchElementException(name)
    }
  }
}

/**
 * Java compatible forwarders.
 */
object KeyHashers {
  val FNV1_32: KeyHasher = KeyHasher.FNV1_32
  val FNV1A_32: KeyHasher = KeyHasher.FNV1A_32
  val FNV1_64: KeyHasher = KeyHasher.FNV1_64
  val FNV1A_64: KeyHasher = KeyHasher.FNV1A_64
  val KETAMA: KeyHasher = KeyHasher.KETAMA
  val CRC32_ITU: KeyHasher = KeyHasher.CRC32_ITU
  val HSIEH: KeyHasher = KeyHasher.HSIEH
  val JENKINS: KeyHasher = KeyHasher.JENKINS
  val MURMUR3: KeyHasher = KeyHasher.MURMUR3

  /**
   * Return one of the key hashing algorithms by name. This is used to configure a memcache
   * client from a config file.
   */
  def byName(name: String): KeyHasher = KeyHasher.byName(name)
}
