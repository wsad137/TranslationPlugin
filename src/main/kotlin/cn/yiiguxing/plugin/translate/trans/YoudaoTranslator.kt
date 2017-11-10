package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.LINK_SETTINGS
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.YOUDAO_TRANSLATE_URL
import cn.yiiguxing.plugin.translate.util.i
import cn.yiiguxing.plugin.translate.util.md5
import cn.yiiguxing.plugin.translate.util.urlEncode
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger

object YoudaoTranslator : AbstractTranslator() {

    @JvmField
    val DEFAULT_SOURCE_LANGUAGE = Lang.AUTO
    @JvmField
    val DEFAULT_TARGET_LANGUAGE = Lang.AUTO

    @JvmField
    val SUPPORTED_LANGUAGES = listOf(
            Lang.AUTO,
            Lang.CHINESE,
            Lang.ENGLISH,
            Lang.JAPANESE,
            Lang.KOREAN,
            Lang.FRENCH,
            Lang.RUSSIAN,
            Lang.PORTUGUESE,
            Lang.SPANISH
    )

    private val LOGGER = Logger.getInstance(YoudaoTranslator::class.java)

    const val TRANSLATOR_ID = "YouDao"

    override val id: String = TRANSLATOR_ID

    private val mSettings = Settings.instance

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun getTranslateUrl(text: String, srcLang: Lang, targetLang: Lang): String {
        val settings = mSettings
        val appId = settings.youdaoTranslateSettings.appId
        val privateKey = settings.youdaoTranslateSettings.getAppKey()
        val salt = System.currentTimeMillis().toString()
        val sign = (appId + text + salt + privateKey).md5()

        return StringBuilder(YOUDAO_TRANSLATE_URL)
                .append("?appKey=", appId.urlEncode(),
                        "&from=", getLanguageCode(srcLang),
                        "&to=", getLanguageCode(targetLang),
                        "&salt=", salt,
                        "&sign=", sign,
                        "&q=", text.urlEncode())
                .toString()
                .also {
                    LOGGER.i("Translate url: $it")
                }
    }

    private fun getLanguageCode(lang: Lang): String = if (lang == Lang.CHINESE) "zh-CHS" else lang.code

    override fun parserResult(result: String): Translation {
        LOGGER.i("Translate result: $result")

        return Gson().fromJson(result, YoudaoResult::class.java).run {
            checkError()
            if (errorCode != 0) {
                throw YoudaoTranslateException(errorCode)
            }

            toTranslation()
        }
    }

    override fun createErrorMessage(throwable: Throwable): String = when (throwable) {
        is YoudaoTranslateException -> "错误[${throwable.code}]: " + when (throwable.code) {
            101 -> "缺少必填的参数"
            102 -> "不支持的语言类型"
            103 -> "翻译文本过长"
            104 -> "不支持的API类型"
            105 -> "不支持的签名类型"
            106 -> "不支持的响应类型"
            107 -> "不支持的传输加密类型"
            108 -> "AppKey无效 - $LINK_SETTINGS"
            109 -> "BatchLog格式不正确"
            110 -> "无相关服务的有效实例"
            111 -> "账号无效或者账号已欠费 - $LINK_SETTINGS"
            201 -> "解密失败"
            202 -> "签名检验失败 - $LINK_SETTINGS"
            203 -> "访问IP地址不在可访问IP列表"
            301 -> "辞典查询失败"
            302 -> "翻译查询失败"
            303 -> "服务器异常"
            401 -> "账户已经欠费"
            else -> "未知错误"
        }
        else -> super.createErrorMessage(throwable)
    }

    class YoudaoTranslateException(val code: Int) : TranslateException("Translate failed: $code")

}