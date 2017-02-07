package com.hrocloud.tiangong.filegw.service

import com.hrocloud.tiangong.filegw.api.FileTokenService
import java.lang

import org.slf4j.LoggerFactory
import com.hrocloud.apigw.client.utils.{AesHelper, Base64Util}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.springframework.stereotype.Service

/**
 * FileTokenServiceBean
 *
 * @author Sean Gao
 */
@Service("fileTokenService")
class FileTokenServiceBean extends FileTokenService {

  val logger = LoggerFactory.getLogger(classOf[FileTokenServiceBean])
  val config = ConfigFactory.load("config")
  val aesToken = config.getString("api.token.aes")
  val aesHelper = new AesHelper(Base64Util.decode(aesToken), null)

  logger.info("fileTokenService init")

  override def requestFileToken(domainId: lang.Long, userId: lang.Long, group : String, fileKey: String, expireAt: Long): String = {
    val _toBeEncoded = s"$domainId|$userId|$group|$fileKey|$expireAt"
    Base64Util.encodeToString(aesHelper.encrypt(_toBeEncoded.getBytes))
  }

}
