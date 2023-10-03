package de.tectoast.emolga.buttons

import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.draft.TipGamedayData
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.litote.kmongo.eq

object TipGameButton : ButtonListener("tipgame") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        //primary("tipgame;${league.name}:$index:${u1.indexedBy(table)}", names[u1]),
        val split = name.split(":")
        val (gameday, index, userindex) = split.drop(1).map { it.toInt() }
        val league =
            db.drafts.findOne(League::leaguename eq split[0]) ?: return reportMissing(e)
        val tipgame = league.tipgame ?: return reportMissing(e)
        val usermap =
            tipgame.tips.getOrPut(gameday) { TipGamedayData() }.userdata.getOrPut(e.user.idLong) { mutableMapOf() }
        usermap[index] = userindex
        e.reply("Dein Tipp wurde gespeichert!").setEphemeral(true).queue()
        league.save()
    }

    private fun reportMissing(e: ButtonInteractionEvent) {
        e.reply("Dieses Tippspiel existiert nicht mehr!")
            .setEphemeral(true).queue()
    }
}
