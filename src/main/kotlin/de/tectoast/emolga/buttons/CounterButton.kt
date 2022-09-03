package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.ifMatches
import de.tectoast.emolga.utils.json.Shinycount
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class CounterButton : ButtonListener("counter") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val split = name.split(":")
        val method = split[0]
        val mem = e.user.idLong.ifMatches(598199247124299776) { it == 893773494578470922 }.toString()
        Shinycount.get.counter[method]!!.compute(mem) { _, v -> (v ?: 0) + split[1].toInt() }
        Command.updateShinyCounts(e)
    }
}