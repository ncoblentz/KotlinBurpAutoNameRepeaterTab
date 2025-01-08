import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.ui.contextmenu.AuditIssueContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider
import burp.api.montoya.ui.contextmenu.WebSocketContextMenuEvent
import com.nickcoblentz.montoya.LogLevel
import com.nickcoblentz.montoya.MontoyaLogger
import java.awt.Component
import javax.swing.JMenuItem


/* Uncomment this section if you wish to use persistent settings and automatic UI Generation from: https://github.com/ncoblentz/BurpMontoyaLibrary
import com.nickcoblentz.montoya.settings.*
import de.milchreis.uibooster.model.Form
import de.milchreis.uibooster.model.FormBuilder
*/

// Montoya API Documentation: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html
// Montoya Extension Examples: https://github.com/PortSwigger/burp-extensions-montoya-api-examples

class KotlinBurpAutoNameRepeaterTabExtension : BurpExtension, ContextMenuItemsProvider {
    private lateinit var api: MontoyaApi
    private lateinit var logger: MontoyaLogger
    private val sendToRepeaterMenuItem = JMenuItem("Send To Repeater")
    private val sendToOrganizerMenuItem = JMenuItem("Send To Organizer")
    private val addBaseURLToScopeMenuItem = JMenuItem("Add base URL to Scope")
    private var requestResponses = emptyList<HttpRequestResponse>()


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

        /* Uncomment this section if you wish to use persistent settings and automatic UI Generation from: https://github.com/ncoblentz/BurpMontoyaLibrary

        exampleNameSetting = StringExtensionSetting(
            // pass the montoya API to the setting
            api,
            // Give the setting a name which will show up in the Swing UI Form
            "My Example Setting Name Here",
            // Key for where to save this setting in Burp's persistence store
            "MyPluginName.ExampleSettingNameHere",
            // Default value within the Swing UI Form
            "default value here",
            // Whether to save it for this specific "PROJECT" or as a global Burp "PREFERENCE"
            ExtensionSettingSaveLocation.PROJECT
            )


        // Create a list of all the settings defined above
        // Don't forget to add more settings here if you define them above
        val extensionSetting = listOf(exampleNameSetting)

        val gen = GenericExtensionSettingsFormGenerator(extensionSetting, "Jwt Token Handler")
        val settingsFormBuilder: FormBuilder = gen.getSettingsFormBuilder()
        val settingsForm: Form = settingsFormBuilder.run()

        // Tell Burp we want a right mouse click context menu for accessing the settings
        api.userInterface().registerContextMenuItemsProvider(ExtensionSettingsContextMenuProvider(api, settingsForm))

        // When we unload this extension, include a callback that closes any Swing UI forms instead of just leaving them still open
        api.extension().registerUnloadingHandler(ExtensionSettingsUnloadHandler(settingsForm))
        */

        // Name our extension when it is displayed inside of Burp Suite
        api.extension().setName("Auto Name Repeater Tab")

        // Code for setting up your extension starts here...

        // Just a simple hello world to start with
        api.userInterface().registerContextMenuItemsProvider(this)
        sendToRepeaterMenuItem.addActionListener {_ -> sendToRepeater() }
        sendToOrganizerMenuItem.addActionListener {_ -> sendToOrganizer() }
        addBaseURLToScopeMenuItem.addActionListener  {_ -> addToScope() }

        // Code for setting up your extension ends here

        // See logging comment above
        logger.debugLog("...Finished loading the extension")

    }

    private fun addToScope() {
        if(requestResponses.isNotEmpty()) {
            for(requestResponse in requestResponses) {
                api.scope().includeInScope(requestResponse.request().url().replace(requestResponse.request().path(),""));
            }
        }
    }

    private fun sendToOrganizer() {
        if(requestResponses.isNotEmpty()) {
            for(requestResponse in requestResponses) {
                api.organizer().sendToOrganizer(requestResponse)
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
                return mutableListOf(sendToRepeaterMenuItem, sendToOrganizerMenuItem, addBaseURLToScopeMenuItem)
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