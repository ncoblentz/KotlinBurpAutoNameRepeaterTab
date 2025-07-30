import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.HighlightColor
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.ui.contextmenu.AuditIssueContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider
import burp.api.montoya.ui.contextmenu.WebSocketContextMenuEvent
import burp.api.montoya.ui.settings.SettingsPanelBuilder
import burp.api.montoya.ui.settings.SettingsPanelPersistence
import com.nickcoblentz.montoya.LogLevel
import com.nickcoblentz.montoya.MontoyaLogger
import com.nickcoblentz.montoya.settings.PanelSettingsDelegate
import java.awt.Component
import javax.swing.JMenuItem
import burp.api.montoya.core.Annotations


// Montoya API Documentation: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html
// Montoya Extension Examples: https://github.com/PortSwigger/burp-extensions-montoya-api-examples

class KotlinBurpAutoNameRepeaterTabExtension : BurpExtension, ContextMenuItemsProvider {
    private lateinit var api: MontoyaApi
    private lateinit var logger: MontoyaLogger
    private val sendToRepeaterMenuItem = JMenuItem("Send To Repeater")
    private val sendToOrganizerMenuItem = JMenuItem("Send To Organizer")
    private val includeBaseURLInScopeMenuItem = JMenuItem("Add Base URL to Scope")
    private val excludeBaseURLFromScopeMenuItem = JMenuItem("Exclude Base URL from Scope")
    private var requestResponses = emptyList<HttpRequestResponse>()
    private lateinit var myExtensionSettings : MyExtensionSettings
    private var organizerCounter = 0

    // Uncomment this section if you wish to use persistent settings and automatic UI Generation from: https://github.com/ncoblentz/BurpMontoyaLibrary
    // Add one or more persistent settings here
    // private lateinit var exampleNameSetting : StringExtensionSetting

    override fun initialize(api: MontoyaApi?) {

        // In Kotlin, you have to explicitly define variables as nullable with a ? as in MontoyaApi? above
        // This is necessary because the Java Library allows null to be passed into this function
        // requireNotNull is a built-in Kotlin function to check for null that throws an Illegal Argument exception if it is null
        // after checking for null, the Kotlin compiler knows that any reference to api  or this.api below will not = null and you no longer have to check it
        // Finally, assign the MontoyaApi instance (not nullable) to a class property to be accessible from other functions in this class
        this.api = requireNotNull(api) { "api : MontoyaApi is not allowed to be null" }

        logger = MontoyaLogger(api, LogLevel.DEBUG)

        // This will print to Burp Suite's Extension output and can be used to debug whether the extension loaded properly
        logger.debugLog("Started loading the extension...")



        api.extension().setName("Auto Name Repeater Tab")


        myExtensionSettings = MyExtensionSettings()
        api.userInterface().registerSettingsPanel(myExtensionSettings.settingsPanel)

        // Just a simple hello world to start with
        api.userInterface().registerContextMenuItemsProvider(this)
        sendToRepeaterMenuItem.addActionListener {_ -> sendToRepeater() }
        sendToOrganizerMenuItem.addActionListener {_ -> sendToOrganizer() }
        includeBaseURLInScopeMenuItem.addActionListener  { _ -> includeInScope() }
        excludeBaseURLFromScopeMenuItem.addActionListener  {_ -> excludeFromScope() }

        // Code for setting up your extension ends here

        // See logging comment above
        logger.debugLog("...Finished loading the extension")

    }

    private fun includeInScope() {
        if(requestResponses.isNotEmpty()) {
            for(requestResponse in requestResponses) {
                val url = getBasURL(requestResponse)
                api.logging().logToOutput(requestResponse.request().url())
                api.logging().logToOutput(url)
                api.scope().includeInScope(url);
            }
        }
    }

    private fun excludeFromScope() {
        if(requestResponses.isNotEmpty()) {
            for(requestResponse in requestResponses) {
                val url = getBasURL(requestResponse)
                api.logging().logToOutput(url)
                api.scope().excludeFromScope(url)
            }
        }
    }

    private fun getBasURL(requestResponse: HttpRequestResponse) : String = requestResponse.request().url().replace(requestResponse.request().path().substring(1),"")

    private fun sendToOrganizer() {
        if(requestResponses.isNotEmpty()) {
            if(myExtensionSettings.tagGroupsInOrganizerNotesSetting && requestResponses.size>1) {
                organizerCounter++
            }

            for(requestResponse in requestResponses) {
                val annotationNotesBuilder = buildString {
                    append(myExtensionSettings.prependStringToOrganizerNotesSetting+" ")
                    if(myExtensionSettings.tagGroupsInOrganizerNotesSetting && requestResponses.size>1) {
                        append(" $organizerCounter ")
                    }
                    if(myExtensionSettings.useTitleInOrganizerNotesSetting && requestResponse.hasResponse()) {
                        val body = requestResponse.response().bodyToString()
                        val titleStartString = "<title>"
                        val titleStartIndex = body.indexOf(titleStartString)
                        val titleEndIndex = body.indexOf("</title>")
                        val headStartIndex = body.indexOf("<head>")
                        val headEndIndex = body.indexOf("</head>")
                        if(titleStartIndex != -1 && titleEndIndex != -1 && headStartIndex != -1 && headEndIndex != -1 &&
                            titleStartIndex > headStartIndex && titleEndIndex < headEndIndex) {
                            append(" "+body.substring(titleStartIndex+titleStartString.length,titleEndIndex)+" ")
                        }
                    }
                    append(" "+myExtensionSettings.appendStringToOrganizerNotesSetting)
                }

                val highlightColor = HighlightColor.valueOf(myExtensionSettings.highlightColorForOrganizerSetting)


                api.organizer().sendToOrganizer(requestResponse.withAnnotations(Annotations.annotations(annotationNotesBuilder.toString(),highlightColor)))
            }
        }
    }

    private fun sendToRepeater() {
        if(requestResponses.isNotEmpty()) {
            for(requestResponse in requestResponses) {
                api.repeater().sendToRepeater(requestResponse.request(),extractTabNameFromRequest(requestResponse.request()))
            }
        }
    }

    private fun extractTabNameFromRequest(request : HttpRequest) : String {
        return buildString {
            append(request.method()+" ")
            append(request.pathWithoutQuery()
                .replace("/[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}".toRegex(RegexOption.IGNORE_CASE),"/:uuid")
                .replace("/api/","/")
                .replace("/\\d+".toRegex(),"/:num")
                .replace("/v\\d/".toRegex(),"/")
                )
        }
    }

    override fun provideMenuItems(event: ContextMenuEvent?): MutableList<Component> {
        event?.let {
            requestResponses = if(it.selectedRequestResponses().isNotEmpty()) {
                it.selectedRequestResponses()
            }
            else if(!it.messageEditorRequestResponse().isEmpty) {
                listOf(it.messageEditorRequestResponse().get().requestResponse())
            }
            else {
                emptyList<HttpRequestResponse>()
            }

            if(requestResponses.isNotEmpty()) {
                return mutableListOf(sendToRepeaterMenuItem, sendToOrganizerMenuItem, includeBaseURLInScopeMenuItem, excludeBaseURLFromScopeMenuItem)
            }
        }
        return mutableListOf<Component>()
    }

    override fun provideMenuItems(event: WebSocketContextMenuEvent?): MutableList<Component> {
        return super.provideMenuItems(event)
    }

    override fun provideMenuItems(event: AuditIssueContextMenuEvent?): MutableList<Component> {
        return super.provideMenuItems(event)
    }


}


class MyExtensionSettings {
    val settingsPanelBuilder : SettingsPanelBuilder = SettingsPanelBuilder.settingsPanel()
        .withPersistence(SettingsPanelPersistence.PROJECT_SETTINGS)
        .withTitle("Auto Name Repeater")
        .withDescription("Update Settings")
        .withKeywords("Auto Name")

    private val settingsManager = PanelSettingsDelegate(settingsPanelBuilder)

    val useTitleInOrganizerNotesSetting: Boolean by settingsManager.booleanSetting("Use the webpage title as part of the organizer notes", false)
    val tagGroupsInOrganizerNotesSetting: Boolean by settingsManager.booleanSetting("When items are submitted to organizer together, tag them", false)

    val prependStringToOrganizerNotesSetting: String by settingsManager.stringSetting("Prepend this string to organizer notes", "")
    val appendStringToOrganizerNotesSetting: String by settingsManager.stringSetting("Append this string to organizer notes", "")
    val highlightColorForOrganizerSetting: String by settingsManager.listSetting("Color to highlight in when sending to organizer",HighlightColor.entries.map { it.name }.toMutableList(), HighlightColor.NONE.name)


    val settingsPanel = settingsManager.buildSettingsPanel()


}
