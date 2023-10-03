package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.utils.automation.collection.ModalConfigurators
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

object ModalConfiguratorSelectMenu : MenuListener("modalconfigurator") {
    override suspend fun process(e: StringSelectInteractionEvent, menuname: String?) {
        e.replyModal(
            ModalConfigurators.configurations[menuname]!!.configurator().buildModal(e.values[0].toInt())
        ).queue()
    }
}
