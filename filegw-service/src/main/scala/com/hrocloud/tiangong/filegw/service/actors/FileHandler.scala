package com.hrocloud.tiangong.filegw.service.actors

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import spray.can.Http
import spray.http.HttpHeaders.{Origin, RawHeader, `Access-Control-Allow-Origin`}
import spray.http._
import spray.http.MediaTypes._
import spray.http.HttpMethods._
import spray.routing._
import com.hrocloud.apigw.client.utils.{AESTokenHelper, AesHelper, Base64Util}
import java.net.{URLDecoder, URLEncoder}

import scala.concurrent.duration._

class FileHandler extends Actor with ActorLogging with HttpService {

  val config = context.system.settings.config
  val decodeKey = config.getString("api.token.aes")
  val aesHelper = new AesHelper(Base64Util.decode(decodeKey), null)
  val aesTokenHelper = new AESTokenHelper(aesHelper)
  val normalFileSize = config.getInt("fastdfs.file.size.limit.normal")
  val adminFileSize = config.getInt("fastdfs.file.size.limit.admin")
  var publicFdfsService = new com.hrocloud.tiangong.filegw.util.FdfsService()
  publicFdfsService.setTrackerAddress(config.getString("fastdfs.public.address"))
  publicFdfsService.init()
  var privateFdsfService = new com.hrocloud.tiangong.filegw.util.FdfsService()
  privateFdsfService.setTrackerAddress(config.getString("fastdfs.private.address"))
  privateFdsfService.init()

  def actorRefFactory = context

  def receive = runRoute(route)

  implicit def executionContext = actorRefFactory.dispatcher

  def route: Route = {
    singleSlashRoute ~
      handleRejections(corsAwareRejectionHandler) {
        optionalHeaderValueByName("origin") { originOption =>
          val origin = originOption match {
            case Some(value) => value
            case None => ""
          }
          if (origin != "" && !originAllowed(origin)) {
            reject
          } else {
            respondWithHeaders(
              RawHeader("Access-Control-Allow-Origin", origin),
              RawHeader("Access-Control-Allow-Method", "GET, POST, OPTIONS"),
              RawHeader("Access-Control-Allow-Credentials", "true")
            ) {
//              testDownloadRoute ~
//              testProxiedFileRoute ~
//              proxiedFileRoute ~
              fileRoute ~
              uploadRoute ~
              adminUploadRoute ~
              deleteRoute ~
              options {
                complete("We do support CORS preflight.")
              }
            }
          }
        }
      }
  }

  def originAllowed(origin: String): Boolean = {
    val host = extractHostFromOrigin(origin)
    for (allowedOrigin <- corsAllowedOrigin.split(",")) {
      if (host.endsWith(allowedOrigin)) {
        return true
      }
    }
    false
  }

  def extractHostFromOrigin(origin: String): String = {
    val startsWithHttp = origin.startsWith("http://")
    val startsWithHttps = origin.startsWith("https://")
    val withoutScheme = if (startsWithHttp) origin.substring(7) else if (startsWithHttps) origin.substring(8) else origin
    val lastIndexOfColon = withoutScheme.lastIndexOf(':')
    if (lastIndexOfColon > 0) withoutScheme.substring(0, lastIndexOfColon) else withoutScheme
  }

  val singleSlashRoute: Route = pathSingleSlash {
    get {
      complete {
        {
            <html>
              <h1>欢迎使用filegw文件网关</h1>
              <br/>
              <h2>上传</h2>
              <p><pre>POST /upload?groupId=0</pre></p>
              <p>上传格式为multipart表单。FASTDFS分为两个集群："0"为非受限访问，即文件上传后所有人不论登录与否均可访问；"1"为受限访问，即文件上传后只有被授权用户可以访问。关于如何对用户做授权，参见"获取文件令牌"。</p>
              <p>上传成功后，文件网关将返回类似如下格式的JSON字符串（上传文件名:filekey）：</p>
              <pre>{{"openjdk-small-new.png":"T15yZTB4JT1RCvBVdK.png","openjdk-small.png":"T15RZTB4JT1RCvBVdK.png"}}</pre>
              <p>fastdfsKey，请客户端将groupid和filekey记入到业务库，以便今后关联使用。</p>
              <p>客户端调用示例（命令行版）：</p>
              <pre>curl -X POST --verbose -F "file=@/home/admin/downloads/git-2.11.0/t/test-binary-1.png" "http://filegw.hrocloud.com/upload?groupId=1" --cookie "_tk=N%2F5Lwf9uzPsbpDysRU%2FVE7Bk6Qzq0%2Bc5N4zUlGl8pBo%3D"</pre>
              <br/>
              <h2>下载</h2>
              <p><pre>GET /file?token=xxxxxxxxxx</pre></p>
              <p>除少数已知媒体类型外，其余文件的下载格式统一都是application/octet-stream。访问非受限FASTDFS集群请走单独的nginx通道，本通道只支持授权访问。关于如何对用户做授权，参见"获取文件令牌"。</p>
              <br/>
              <h2>获取文件令牌</h2>
              <p>业务系统在对用户进行验权后，调用dubbo服务获取token参数，拼接到文件下载的URL中，用于文件网关在收到文件下载请求时判定用户身份和请求的合法性。dubbo服务接口定义如下（请添加filegw-api依赖）：</p>
              <pre>/**
                * 请求文件令牌（用于业务服务端对用户文件访问请求验权后，生成专为该用户准备的文件访问令牌）
                *
                * @param domainId
                * @param userId
                * @param group
                * @param fileKey
                * @param expireAt
                *
                * @return 文件访问令牌
                */
                public String requestFileToken(Long domainId, Long userId, String group, String fileKey, long expireAt);
              </pre>
              <br/>
              <h2>关于cookie</h2>
              <p>登录成功后，PC端的用户浏览器会保存cookie，手机端会保留token（用途和浏览器cookie一样）。开发环境<a href="/cookie">点此获取临时cookie</a>，正常打开以后，请在本地浏览器查找名为_tk的cookie。<em><b>由于手机端我们默认关闭cookie功能，请做手机端的同学记得将本地用户token以_tk参数方式拼接在URL中。</b></em></p>
              <p>开发同学遇到HTTP返回码40x或CORS相关错误请确保使用的HTTP方法正确且请求URL中所有参数完整、有效、拼写正确（对于手机端H5而言，需要验_tk参数）。</p>
              <br/>
            </html>

        }
      }
    }
  }


//  val testDownloadRoute: Route = path("testDownload") {
//      detach() {
//        setCookie(HttpCookie("_tk", encodeCookie("1000", "10909", "1"))) & complete {
//          val fileToken = encodeFileToken("1000", "10909", "1", "M00/01/B5/wKgLPViDWp2AFei7AAVzLhEqcT445..png", System.currentTimeMillis() + 3600000)
//          val urlEncoded = "/file?token=" + URLEncoder.encode(fileToken, "UTF-8")
//            <img src={urlEncoded}/>
//        }
//      }
//  }

  val fileRoute: Route = path("file") {
    get {
      parameter("token") { token =>
        parameter("_tk".?) { tkOption =>
          tkOption match {
            case Some(value) =>
              val (_, userId) = decodeUserToken(value)
              download(token, userId)
            case _ =>
              cookie("_tk") { cookie =>
                val (_, userId) = decodeCookie(cookie.content)
                download(token, userId)
              }
          }
        }
      }
    }
  }

  def download(token: String, userId: String): Route = {
    clientIP { _cip =>
      val ip = _cip.toOption.map(_.getHostAddress).getOrElse("unknown")
//      val (_domainId, _userId, _tfsGroup, _tfsKey, _expireAt) = decodeFileToken(token)
      val (_, _userId, groupId, _fileKey, _expireAt) = decodeFileToken(token)
      if (userId != _userId || System.currentTimeMillis() > _expireAt) {
        reject(ValidationRejection("Invalid cookie!"))
      } else {
        val group = groupId match {
          case "0" => "group0"
          case "1" => "group1"
        }

        val (contentType, bytes) = retrieveFile(group, _fileKey)
        respondWithMediaType(contentType.mediaType) {
          complete(bytes)
        }
      }
    }
  }

  def retrieveFile(group: String, fileKey: String): (ContentType, Array[Byte]) = {
    val fdfsService = group match {
      case "group0" =>
        publicFdfsService
      case "group1" =>
        privateFdsfService
    }

    val fileInfo = fdfsService.statFile(fileKey)
    log.info(">>>>>>>>>>" + fileInfo)
    log.info(">>>>>>>>>>" + fileInfo.getFileSize)
    if (fileInfo != null && fileInfo.getFileSize > 0) {
      val bytes = fdfsService.download(fileKey)
      (getContentTypeByTfsKey(fileKey),bytes)
    } else {
      (getContentTypeByTfsKey(fileKey), Array[Byte]())
    }
  }

  def getContentTypeByTfsKey(fileKey: String): ContentType = {
    val lastIndexOfDot = fileKey.lastIndexOf('.')
    val suffix = if (lastIndexOfDot > 0) fileKey.substring(lastIndexOfDot).toLowerCase else ""
    suffix match {
      case ".pdf" =>
        `application/pdf`
      case ".txt" =>
        `text/plain`
      case ".csv" =>
        `text/csv`
      case ".xml" =>
        `text/xml`
      case ".mp3" =>
        `audio/mpeg`
      case ".mp4" =>
        `video/mpeg`
      case ".mov" =>
        `video/quicktime`
      case ".wmv" =>
        `video/x-msvideo`
      case ".flv" =>
        `video/x-flv`
      case ".gif" =>
        `image/gif`
      case ".jpg" =>
        `image/jpeg`
      case ".png" =>
        `image/png`
      case _ =>
        `application/octet-stream`
    }
  }

  val proxiedFileRoute: Route = path("file") {
    get {
      parameter("token") { token =>
        parameter("_tk".?) { tkOption =>
          tkOption match {
            case Some(value) =>
              val (_, userId) = decodeUserToken(value)
              proxyDownload(token, userId)
            case _ =>
              cookie("_tk") { cookie =>
                val (_, userId) = decodeCookie(cookie.content)
                proxyDownload(token, userId)
              }
          }
        }
      }
    }
  }

  val uploadRoute: Route = path("upload") {
    post {
      parameter("groupId") { groupId =>
        parameter("_tk".?) { tkOption =>
          tkOption match {
            case Some(value) =>
              val (_, userId) = decodeUserToken(value)
              upload(groupId, userId)
            case None =>
              cookie("_tk") { cookie =>
                val (_, userId) = decodeCookie(cookie.content)
                upload(groupId, userId)
              }
          }
        }
      }
    }
  }
  
  val deleteRoute: Route = path("file_delete"){
    post{
      parameter("filename"){filename=>
         parameter("groupId") { groupId =>
          parameter("_tk".?) { tkOption =>
            tkOption match {
              case Some(value) =>
                val (_, userId) = decodeUserToken(value)
                deleteFile(groupId, userId,filename)
              case None =>
                cookie("_tk") { cookie =>
                  val (_, userId) = decodeCookie(cookie.content)
                  deleteFile(groupId, userId,filename)
                }
            }
          }
        }
      }
      
    }
  }
  
  def deleteFile(groupId: String, userId: String,filename:String): Route = {
    if (userId != null && userId != "") {
      respondWithMediaType(`application/json`) {
          complete {

             var isSucc:Boolean = false
             val fileOrgName = filename.substring(0, filename.indexOf("."));

              JSON.toJSONString(isSucc,false)
          }
      }
    } else {
      reject(ValidationRejection("Invalid cookie!"))
    }
  }

  val adminUploadRoute: Route = path("admin_upload") {
    post {
      parameter("groupId") { groupId =>
        parameter("_tk".?) { tkOption =>
          tkOption match {
            case Some(value) =>
              val (_, userId) = decodeUserToken(value)
              adminUpload(groupId, userId)
            case None =>
              cookie("_tk") { cookie =>
                val (_, userId) = decodeCookie(cookie.content)
                adminUpload(groupId, userId)
              }
          }
        }
      }
    }
  }

  val corsAwareRejectionHandler = RejectionHandler {
    case Nil =>
      complete(StatusCodes.NotFound, "404, but we do support CORS.")
  }

  val testProxiedFileRoute: Route = path("_testfile") {
    get {
      parameter("key") { tfsKey =>
        detach() {
          testProxyDownload("1", tfsKey)
        }
      }
    }
  }

  def testProxyDownload(groupId: String, _fileKey: String): Route = {
    val proxiedUrl = groupId match {
      case "0" => config.getString("fastdfs.public.url") + _fileKey
      case "1" => config.getString("fastdfs.private.url") + _fileKey
    }
    log.info(s">>>>>>>> proxied url: $proxiedUrl")
    implicit val timeout: Timeout = Timeout(15 seconds)
    implicit val system: ActorSystem = context.system
    complete { (IO(Http) ? HttpRequest(GET, Uri(proxiedUrl))).mapTo[HttpResponse] }
  }

  def proxyDownload(token: String, userId: String): Route = {
    val (_, _userId, groupId, _fileKey, _expireAt) = decodeFileToken(token)
    if (userId != _userId || System.currentTimeMillis() > _expireAt) {
      log.error(s"user ids don't match: ${userId} vs. ${_userId}, or token expired: ${_expireAt}")
      reject(ValidationRejection("invalid cookie or token!"))
    } else {
      val proxiedUrl = groupId match {
        case "0" => config.getString("fastdfs.public.url") + _fileKey
        case "1" => config.getString("fastdfs.private.url") + _fileKey
      }
      log.info(s">>>>>>>> proxied url: $proxiedUrl")
      implicit val timeout: Timeout = Timeout(15 seconds)
      implicit val system: ActorSystem = context.system
      complete { (IO(Http) ? HttpRequest(GET, Uri(proxiedUrl))).mapTo[HttpResponse] }
    }
  }

  def upload(groupId: String, userId: String): Route = {
    if (userId != null && userId != "") {
      respondWithMediaType(`application/json`) {
        entity(as[MultipartFormData]) { formData =>
          complete {
            groupId match {
              case "0" =>
                log.info(s"use public file cloud.")
                saveFiles("group0", formData)
              case "1" =>
                log.info(s"use private file cloud.")
                saveFiles("group1", formData)
            }
          }
        }
      }
    } else {
      reject(ValidationRejection("Invalid cookie!"))
    }
  }

  def adminUpload(groupId: String, userId: String): Route = {
    if (userId != null && userId != "") {
      respondWithMediaType(`application/json`) {
        entity(as[MultipartFormData]) { formData =>
          complete {
            groupId match {
              case "0" =>
                saveFiles("group0", formData, true)
              case "1" =>
                saveFiles("group1", formData, true)
            }
          }
        }
      }
    } else {
      reject(ValidationRejection("Invalid cookie!"))
    }
  }

  def saveFiles(group: String, formData: MultipartFormData, admin: Boolean = false): String = {


    val fdfsService = group match {
      case "group0" =>
        publicFdfsService
      case "group1" =>
        privateFdsfService
    }

    val files: java.util.Map[String, Object] = new java.util.HashMap[String, Object]()

    formData.fields.foreach {
      case BodyPart(entity, headers) if headers.exists(h => h.is("content-type")) =>
        val contentType = headers.find(h => h.is("content-type"))
        val contentDisposition = headers.find(h => h.is("content-disposition"))
        val rawFileName = new java.lang.String(contentDisposition.get.value.split("filename=").last.getBytes("ISO-8859-1"), "UTF-8")
        val fileName = if (rawFileName.startsWith("\"") && rawFileName.endsWith("\"")) rawFileName.substring(1, rawFileName.length - 1) else rawFileName
        val data = entity.data.toByteArray
        if (data != null && data.size > 0 && data.size <= (if (admin) adminFileSize else normalFileSize)) {
          try {
            val lastIndexOfDot = fileName.lastIndexOf('.')
            val suffix = if (lastIndexOfDot > 0) fileName.substring(lastIndexOfDot+1) else ""
            log.info(s"suffix is $suffix")

            val result = fdfsService.save(entity.data.toByteArray,suffix)
            files.put(fileName, result)
            log.info(s"Fdfsservice.save() to $group successful. File name: $fileName, size: ${data.size}, result: $result.")
          } catch {
            case ex: Exception =>
              log.error(s"Fdfsservice.save() to $group failed. File name: $fileName, size: ${data.size}.")
          }
        }
      case _ =>
    }

    JSON.toJSONString(files, SerializerFeature.WriteMapNullValue)
  }



  def getContentType(fileKey: String): ContentType = {
    val lastIndexOfDot = fileKey.lastIndexOf('.')
    val suffix = if (lastIndexOfDot > 0) fileKey.substring(lastIndexOfDot).toLowerCase else ""
    suffix match {
      case ".pdf" =>
        `application/pdf`
      case ".txt" =>
        `text/plain`
      case ".csv" =>
        `text/csv`
      case ".xml" =>
        `text/xml`
      case ".mp3" =>
        `audio/mpeg`
      case ".mp4" =>
        `video/mpeg`
      case ".mov" =>
        `video/quicktime`
      case ".wmv" =>
        `video/x-msvideo`
      case ".flv" =>
        `video/x-flv`
      case ".gif" =>
        `image/gif`
      case ".jpg" =>
        `image/jpeg`
      case ".png" =>
        `image/png`
      case _ =>
        `application/octet-stream`
    }
  }

  def decodeCookie(cookieValue: String): (String, String) = { // (domainId, userId)
    var domainId = ""
    var userId = ""
    try {
      if (cookieValue != null && !"".equals(cookieValue)) {
        val cookieKey = new java.lang.String(aesHelper.decrypt(Base64Util.decode(URLDecoder.decode(cookieValue, "UTF-8"))))
        log.info(s">>>>>>>>>>$cookieKey")
        if (cookieKey != null && cookieKey.split("\\|").length >= 2) {
          val parts = cookieKey.split("\\|")
          domainId = parts(0)
          userId = parts(1)
        } else {
          log.error(s"Error decoding cookie [$cookieKey].")
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"Error decoding cookie [$cookieValue], possible breaking attempt! Exception was $ex.")
    }
    (domainId, userId)
  }

  def decodeUserToken(userToken: String): (String, String) = { // (domainId, userId)
    var domainId = ""
    var userId = ""
    try {
      if (userToken != null && !"".equals(userToken)) {
        val cookieKey = new java.lang.String(aesHelper.decrypt(Base64Util.decode(userToken)))
        log.info(s">>>>>>>>>>$cookieKey")
        if (cookieKey != null && cookieKey.split("\\|").length >= 2) {
          val parts = cookieKey.split("\\|")
          domainId = parts(0)
          userId = parts(1)
        } else {
          log.error(s"Error decoding cookie [$cookieKey].")
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"Error decoding cookie [$userToken], possible breaking attempt! Exception was $ex.")
    }
    (domainId, userId)
  }

  def encodeCookie(domainId: String, userId: String, appId: String): String = {
    URLEncoder.encode(Base64Util.encodeToString(aesHelper.encrypt((domainId + "|" + userId + "|0|" + System.currentTimeMillis() + "|||||").getBytes)), "UTF-8")
  }

  def decodeFileToken(fileToken: String): (String, String, String, String, Long) = { // (domainId, userId, group, fileKey, expireAt)
    val decoded = new java.lang.String(aesHelper.decrypt(Base64Util.decode(fileToken)))
    var domainId = ""
    var userId = ""
    var group = ""
    var fileKey = ""
    var expireAt = 0L
    try {
      if (decoded != null && decoded.split("\\|").length >= 5) {
        val parts = decoded.split("\\|")
        domainId = parts(0)
        userId = parts(1)
        group = parts(2)
        fileKey = parts(3)
        expireAt = java.lang.Long.parseLong(parts(4))
      }
    } catch {
      case ex: Throwable => log.error("Error decoding file token, possible breaking attempt!")
    }
    (domainId, userId, group, fileKey, expireAt)
  }

  def encodeFileToken(domainId: String, userId: String, group: String, fileKey: String, expireAt: Long): String = {
    val _toBeEncoded = s"$domainId|$userId|$group|$fileKey|$expireAt"
    Base64Util.encodeToString(aesHelper.encrypt(_toBeEncoded.getBytes))
  }

  def corsAllowedOrigin: String = config.getString("spray.cors.allowed.origin")

}
