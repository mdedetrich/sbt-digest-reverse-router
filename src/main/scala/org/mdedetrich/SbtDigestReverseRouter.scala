package org.mdedetrich

import java.io.InputStream

import com.typesafe.config.{Config, ConfigFactory}
import java.security.{DigestInputStream, MessageDigest}
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

trait SbtDigestReverseRouter {

  private val memoizeMap = new ConcurrentHashMap[String, String]()

  private val config: Config = ConfigFactory.load()
  private val configPrefix = "sbt-digest-reverse-router"

  private val digestAlgorithm =
    config.getString(s"$configPrefix.digest-algorithm")
  private val packagePrefix = config.getString(s"$configPrefix.package-prefix")

  // Efficient way to generate a checksum from an input stream https://stackoverflow.com/a/304350
  // Byte Array to hex algorithm taken from http://rgagnon.com/javadetails/java-0596.html
  private final def digestFromInputStream(inputStream: InputStream,
                                          algorithm: String): String = {
    val md = MessageDigest.getInstance(algorithm)
    val digestInputStream = new DigestInputStream(inputStream, md)

    var length = digestInputStream.read()

    while (length != -1) {
      length = digestInputStream.read()
    }

    val sb = new StringBuffer

    val digested = md.digest()
    digestInputStream.close()

    var i = 0

    while (i < digested.length) {
      sb.append(
        Integer.toString((digested(i) & 0xff) + 0x100, 16).substring(1)
      )
      i += 1
    }

    sb.toString
  }

  private def generateMD5FromInputStream(inputStream: InputStream): String = {
    digestFromInputStream(inputStream, "MD5")
  }

  private def generateSHA1FromInputStream(inputStream: InputStream): String = {
    digestFromInputStream(inputStream, "SHA-1")
  }

  private final def finalPath(path: String): String = path match {
    case ""    => ""
    case other => s"$other"
  }

  private def generateChecksum(inputStream: InputStream,
                               algorithm: String): String = {
    algorithm match {
      case "sha1" => generateSHA1FromInputStream(inputStream)
      case "md5"  => generateMD5FromInputStream(inputStream)
      case _      => throw SbtDigestReverseRouter.UnknownDigestAlgorithm(algorithm)
    }
  }

  // Efficient way to convert input stream to string https://stackoverflow.com/a/35446009/1519631

  private final def getChecksumByFile(path: String,
                                      assetName: String,
                                      algorithm: String): Option[String] = {
    val fileName = s"/$packagePrefix${finalPath(path)}$assetName.$algorithm"
    try {
      val inputStream = getClass.getResourceAsStream(fileName)
      val result = new ByteArrayOutputStream
      val buffer = new Array[Byte](8096)
      var length = 0
      length = inputStream.read(buffer)

      while (length != -1) {
        result.write(buffer, 0, length)
        length = inputStream.read(buffer)
      }

      val output = result.toString()

      inputStream.close()
      Some(output.trim)
    } catch {
      case _: NullPointerException => None // Gets thrown if file doesn't exist
    }
  }

  private final def getChecksumByCalculation(
      path: String,
      assetName: String,
      algorithm: String): Option[String] = {
    val fileName = s"/$packagePrefix${finalPath(path)}$assetName"

    try {
      val inputStream = getClass.getResourceAsStream(fileName)
      println(fileName)
      Some(generateChecksum(inputStream, algorithm))
    } catch {
      case _: NullPointerException => None // Gets thrown if file doesn't exist
    }

  }

  @throws[SbtDigestReverseRouter.ResourceNotFound](
    "If resource for this asset cannot be found")
  @throws[SbtDigestReverseRouter.UnknownDigestAlgorithm](
    "If using algorithm that is not md5 or sha1")
  final def findVersionedAsset(assetFileName: String): String =
    findVersionedAsset("", assetFileName, digestAlgorithm)

  @throws[SbtDigestReverseRouter.ResourceNotFound](
    "If resource for this asset cannot be found")
  @throws[SbtDigestReverseRouter.UnknownDigestAlgorithm](
    "If using algorithm that is not md5 or sha1")
  final def findVersionedAsset(path: String, assetFileName: String): String =
    findVersionedAsset(path, assetFileName, digestAlgorithm)

  @throws[SbtDigestReverseRouter.ResourceNotFound](
    "If resource for this asset cannot be found")
  @throws[SbtDigestReverseRouter.UnknownDigestAlgorithm](
    "If using algorithm that is not md5 or sha1")
  final def findVersionedAsset(path: String,
                               assetFileName: String,
                               algorithm: String): String = {
    val fPath = finalPath(path)

    val leadingPath = path match {
      case ""    => ""
      case other => s"$other"
    }

    val key = s"$fPath$assetFileName"

    val getValue = memoizeMap.get(key)

    val digested = if (getValue != null) {
      getValue
    } else {
      val checkSum =
        (getChecksumByFile(path, assetFileName, algorithm) orElse getChecksumByCalculation(
          path,
          assetFileName,
          algorithm)).getOrElse(
          throw SbtDigestReverseRouter.ResourceNotFound(key)
        )

      val maybeChecksum = memoizeMap.putIfAbsent(key, checkSum)

      if (maybeChecksum != null)
        maybeChecksum
      else
        checkSum
    }

    s"$leadingPath$digested-$assetFileName"
  }
}

object SbtDigestReverseRouter extends SbtDigestReverseRouter {
  case class UnknownDigestAlgorithm(algorithm: String) extends Exception {
    override def getMessage: String = s"Unknown digest algorithm: $algorithm"
  }
  case class ResourceNotFound(resourcePath: String) extends Exception {
    override def getMessage: String =
      s"Unable to find resource at path $resourcePath"
  }
}
