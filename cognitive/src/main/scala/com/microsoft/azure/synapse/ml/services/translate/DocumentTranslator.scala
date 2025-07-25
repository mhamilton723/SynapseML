// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.azure.synapse.ml.services.translate

import com.microsoft.azure.synapse.ml.build.BuildInfo
import com.microsoft.azure.synapse.ml.codegen.Wrappable
import com.microsoft.azure.synapse.ml.services._
import com.microsoft.azure.synapse.ml.services.search.HasServiceName
import com.microsoft.azure.synapse.ml.services.vision.BasicAsyncReply
import com.microsoft.azure.synapse.ml.io.http.HandlingUtils.{convertAndClose, sendWithRetries}
import com.microsoft.azure.synapse.ml.io.http.{HTTPResponseData, HeaderValues}
import com.microsoft.azure.synapse.ml.logging.{FeatureNames, SynapseMLLogging}
import com.microsoft.azure.synapse.ml.param.ServiceParam
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.{AbstractHttpEntity, ContentType, StringEntity}
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.spark.ml.ComplexParamsReadable
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import spray.json._

import java.net.URI
import scala.reflect.internal.util.ScalaClassLoader

trait DocumentTranslatorAsyncReply extends BasicAsyncReply {

  import TranslatorJsonProtocol._

  override protected def queryForResult(headers: Map[String, String],
                                        client: CloseableHttpClient,
                                        location: URI): Option[HTTPResponseData] = {
    val get = new HttpGet()
    get.setURI(location)
    headers.foreach { case (k, v) => get.setHeader(k, v) }
    get.setHeader("User-Agent", s"synapseml/${BuildInfo.version}${HeaderValues.PlatformInfo}")
    val resp = convertAndClose(sendWithRetries(client, get, getBackoffs))
    get.releaseConnection()
    val status = IOUtils.toString(resp.entity.get.content, "UTF-8")
      .parseJson.asJsObject.fields.get("status").map(_.convertTo[String])
    status.map(_.toLowerCase()).flatMap {
      case "succeeded" | "failed" | "canceled" | "ValidationFailed" => Some(resp)
      case "notstarted" | "running" | "cancelling" => None
      case s => throw new RuntimeException(s"Received unknown status code: $s")
    }
  }
}

object DocumentTranslator extends ComplexParamsReadable[DocumentTranslator]

class DocumentTranslator(override val uid: String) extends CognitiveServicesBaseNoHandler(uid)
  with HasInternalJsonOutputParser with HasCognitiveServiceInput with HasServiceName
  with Wrappable with DocumentTranslatorAsyncReply with SynapseMLLogging with HasSetLinkedService
  with DomainHelper {

  import TranslatorJsonProtocol._

  logClass(FeatureNames.AiServices.Translate)

  def this() = this(Identifiable.randomUID("DocumentTranslator"))

  val filterPrefix = new ServiceParam[String](
    this, "filterPrefix", "A case-sensitive prefix string to filter documents in the source" +
      " path for translation. For example, when using an Azure storage blob Uri, use the prefix to" +
      " restrict sub folders for translation.")

  def setFilterPrefix(v: String): this.type = setScalarParam(filterPrefix, v)

  def setFilterPrefixCol(v: String): this.type = setVectorParam(filterPrefix, v)

  val filterSuffix = new ServiceParam[String](
    this, "filterSuffix", "A case-sensitive suffix string to filter documents in the source" +
      " path for translation. This is most often use for file extensions.")

  def setFilterSuffix(v: String): this.type = setScalarParam(filterSuffix, v)

  def setFilterSuffixCol(v: String): this.type = setVectorParam(filterSuffix, v)

  val sourceLanguage = new ServiceParam[String](this, "sourceLanguage", "Language code." +
    " If none is specified, we will perform auto detect on the document.")

  def setSourceLanguage(v: String): this.type = setScalarParam(sourceLanguage, v)

  def setSourceLanguageCol(v: String): this.type = setVectorParam(sourceLanguage, v)

  val sourceUrl = new ServiceParam[String](this, "sourceUrl", "Location of the folder /" +
    " container or single file with your documents.", isRequired = true)

  def setSourceUrl(v: String): this.type = setScalarParam(sourceUrl, v)

  def setSourceUrlCol(v: String): this.type = setVectorParam(sourceUrl, v)

  val sourceStorageSource = new ServiceParam[String](this, "sourceStorageSource",
    "Storage source of source input.")

  def setSourceStorageSource(v: String): this.type = setScalarParam(sourceStorageSource, v)

  def setSourceStorageSourceCol(v: String): this.type = setVectorParam(sourceStorageSource, v)

  val storageType = new ServiceParam[String](this, "storageType", "Storage type of the input" +
    " documents source string. Required for single document translation only.")

  def setStorageType(v: String): this.type = setScalarParam(storageType, v)

  def setStorageTypeCol(v: String): this.type = setVectorParam(storageType, v)

  val targets = new ServiceParam[Seq[TargetInput]](this, "targets", "Destination for the" +
    " finished translated documents.")

  def setTargets(v: Seq[TargetInput]): this.type = setScalarParam(targets, v)

  def setTargetsCol(v: String): this.type = setVectorParam(targets, v)

  override protected def prepareEntity: Row => Option[AbstractHttpEntity] = {
    def fetchGlossaries(row: Row): Option[Seq[Glossary]] = {
      try {
        Option(row.getSeq(1).asInstanceOf[Seq[Row]].map(
          x => Glossary(x.getString(0), x.getString(1), Option(x.getString(2)), Option(x.getString(3)))
        ))
      } catch {
        case _: NullPointerException => Option(row.getAs[Seq[Glossary]]("glossaries"))
      }
    }

    r =>
      Some(new StringEntity(
        Map("inputs" -> Seq(
          BatchRequest(source = SourceInput(
            filter = Option(DocumentFilter(
              prefix = getValueOpt(r, filterPrefix),
              suffix = getValueOpt(r, filterSuffix))),
            language = getValueOpt(r, sourceLanguage),
            storageSource = getValueOpt(r, sourceStorageSource),
            sourceUrl = getValue(r, sourceUrl)),
            storageType = getValueOpt(r, storageType),
            targets = getValue(r, targets).asInstanceOf[Seq[Row]].map(
              row => TargetInput(Option(row.getString(0)),
                fetchGlossaries(row),
                row.getString(2), row.getString(3), Option(row.getString(4))))
          ))).toJson.compactPrint, ContentType.APPLICATION_JSON))
  }

  override def setLinkedService(v: String): this.type = {
    val classPath = "mssparkutils.cognitiveService"
    val linkedServiceClass = ScalaClassLoader(getClass.getClassLoader).tryToLoadClass(classPath)
    val nameMethod = linkedServiceClass.get.getMethod("getName", v.getClass)
    val keyMethod = linkedServiceClass.get.getMethod("getKey", v.getClass)
    val name = nameMethod.invoke(linkedServiceClass.get, v).toString
    val key = keyMethod.invoke(linkedServiceClass.get, v).toString
    setServiceName(name)
    setSubscriptionKey(key)
  }

  override def setServiceName(v: String): this.type = {
    super.setServiceName(v)
    val domain = getLocationDomain(v)
    setUrl(s"https://$getServiceName.cognitiveservices.azure.$domain/" + urlPath)
  }

  def urlPath: String = "/translator/text/batch/v1.0/batches"

  override def transform(dataset: Dataset[_]): DataFrame = {
    logTransform[DataFrame]({
      getInternalTransformer(dataset.schema).transform(dataset)
    }, dataset.columns.length)
  }

  override def responseDataType: DataType = TranslationStatusResponse.schema
}
