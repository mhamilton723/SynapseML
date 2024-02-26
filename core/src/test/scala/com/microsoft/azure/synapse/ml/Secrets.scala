// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.azure.synapse.ml

import spray.json.DefaultJsonProtocol._
import spray.json._

import java.io.IOException
import scala.sys.process._

object Secrets {
  private val KvName = "mmlspark-build-keys"
  private[ml] val SubscriptionID = "e342c2c0-f844-4b18-9208-52c8c234c30e"

  protected def exec(command: String): String = {
    val os = sys.props("os.name").toLowerCase
    os match {
      case x if x contains "windows" => Seq("cmd", "/C") ++ Seq(command) !!
      case _ => command !!
    }
  }

  // Keep overhead of setting account down
  lazy val AccountString: String = {
    try {
      exec(s"az account set -s $SubscriptionID")
    } catch {
      case e: java.lang.RuntimeException =>
        println(s"Secret fetch error: ${e.toString}")
      case e: IOException =>
        println(s"Secret fetch error: ${e.toString}")
    }
    SubscriptionID
  }

  def getSynapseExtensionSecret(envName: String, secretType: String): String = {
    val secretKey = s"synapse-extension-$envName-$secretType"
    println(s"[info] fetching secret: $secretKey from $AccountString")
    val secretJson = exec(s"az keyvault secret show --vault-name $KvName --name $secretKey")
    secretJson.parseJson.asJsObject().fields("value").convertTo[String]
  }

  private def getSecret(secretName: String): String = {
    println(s"[info] fetching secret: $secretName from $AccountString")
    val secretJson = exec(s"az keyvault secret show --vault-name $KvName --name $secretName")
    secretJson.parseJson.asJsObject().fields("value").convertTo[String]
  }

  lazy val CognitiveApiKey: String = getSecret("cognitive-api-key")
  lazy val OpenAIApiKey: String = getSecret("openai-api-key")
  lazy val OpenAIApiKeyGpt4: String = getSecret("openai-api-key-2")

  lazy val CustomSpeechApiKey: String = getSecret("custom-speech-api-key")
  lazy val ConversationTranscriptionUrl: String = getSecret("conversation-transcription-url")
  lazy val ConversationTranscriptionKey: String = getSecret("conversation-transcription-key")

  lazy val AnomalyApiKey: String = getSecret("anomaly-api-key")
  lazy val AzureSearchKey: String = getSecret("azure-search-key")
  lazy val BingSearchKey: String = getSecret("bing-search-key")
  lazy val TranslatorKey: String = getSecret("translator-key")
  lazy val AzureMapsKey: String = getSecret("azuremaps-api-key")
  lazy val PowerbiURL: String = getSecret("powerbi-url")
  lazy val AdbToken: String = getSecret("adb-token")
  lazy val SynapseStorageKey: String = getSecret("synapse-storage-key")
  lazy val SynapseSpnKey: String = getSecret("synapse-spn-key")
  lazy val MADTestStorageKey: String = getSecret("madtest-storage-key")

  lazy val ArtifactStore: String = getSecret("synapse-artifact-store")
  lazy val Platform: String = getSecret("synapse-platform")
  lazy val AadResource: String = getSecret("synapse-internal-aad-resource")
  lazy val ServiceConnectionSecret: String = getSecret("service-connection-secret")
  lazy val ServicePrincipalClientId: String = getSecret("service-principal-clientId")

  lazy val SynapseExtensionEdogPassword: String = getSecret("synapse-extension-edog-password")
  lazy val SynapseExtensionEdogTenantId: String = getSecret("synapse-extension-edog-tenant-id")
  lazy val SynapseExtensionEdogUxHost: String = getSecret("synapse-extension-edog-ux-host")
  lazy val SynapseExtensionEdogSspHost: String = getSecret("synapse-extension-edog-ssp-host")
  lazy val SynapseExtensionEdogWorkspaceId: String = getSecret("synapse-extension-edog-workspace-id")

  lazy val SynapseExtensionDailyPassword: String = getSecret("synapse-extension-daily-password")
  lazy val SynapseExtensionDailyTenantId: String = getSecret("synapse-extension-daily-tenant-id")
  lazy val SynapseExtensionDailyUxHost: String = getSecret("synapse-extension-daily-ux-host")
  lazy val SynapseExtensionDailySspHost: String = getSecret("synapse-extension-daily-ssp-host")
  lazy val SynapseExtensionDailyWorkspaceId: String = getSecret("synapse-extension-daily-workspace-id")

  lazy val SynapseExtensionDxtPassword: String = getSecret("synapse-extension-dxt-password")
  lazy val SynapseExtensionDxtTenantId: String = getSecret("synapse-extension-dxt-tenant-id")
  lazy val SynapseExtensionDxtUxHost: String = getSecret("synapse-extension-dxt-ux-host")
  lazy val SynapseExtensionDxtSspHost: String = getSecret("synapse-extension-dxt-ssp-host")
  lazy val SynapseExtensionDxtWorkspaceId: String = getSecret("synapse-extension-dxt-workspace-id")

  lazy val SynapseExtensionMsitPassword: String = getSecret("synapse-extension-msit-password")
  lazy val SynapseExtensionMsitTenantId: String = getSecret("synapse-extension-msit-tenant-id")
  lazy val SynapseExtensionMsitUxHost: String = getSecret("synapse-extension-msit-ux-host")
  lazy val SynapseExtensionMsitSspHost: String = getSecret("synapse-extension-msit-ssp-host")
  lazy val SynapseExtensionMsitWorkspaceId: String = getSecret("synapse-extension-msit-workspace-id")


}
