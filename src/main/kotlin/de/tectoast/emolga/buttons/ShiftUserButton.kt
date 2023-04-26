package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.PrivateCommands
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ShiftUserButton : ButtonListener("shiftuser") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val gid = e.guild!!.idLong
        if (gid !in Emolga.get.signups) {
            e.reply("Diese Anmeldung ist bereits geschlossen!").setEphemeral(true).queue()
            return
        }
        with(Emolga.get.signups[gid]!!) {
            val uid = name.toLong()
            if (extended) {
                e.reply_(
                    embeds = Embed(title = "Shift", description = "<@$uid>").into(),
                    components = conferenceSelectMenus(uid, false).into()
                ).queue()
                return
            }
            e.deferEdit().queue()
            val tc = e.jda.getTextChannelById(shiftChannel!!)!!
            val user = users[uid]!!
            val currConf = user.conference!!
            user.conference = conferences[(conferences.indexOf(currConf) + 1) % conferences.size]
            PrivateCommands.generateOrderingMessages(this).forEach { (index, pair) ->
                tc.editMessage(shiftMessageIds[index].toString(), embeds = pair.first.into(), components = pair.second)
                    .queue()
            }
            saveEmolgaJSON()
        }
    }
}
