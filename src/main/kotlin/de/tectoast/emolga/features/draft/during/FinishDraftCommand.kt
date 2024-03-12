package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.json.emolga.draft.League

object FinishDraftCommand :
    CommandFeature<NoArgs>(NoArgs(), CommandSpec("finishdraft", "Beendet für dich den Draft", *draftGuilds)) {
    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        val mem = user
        val d = League.onlyChannel(tc)?.takeIf { mem in it.table } ?: return reply(
            "In diesem Channel läuft kein Draft, an welchem du teilnimmst!",
            ephemeral = true
        )
        d.lock {
            if (isFinishedForbidden()) return reply("Dieser Draft unterstützt /finishdraft nicht!")
            checkFinishedForbidden(mem)?.let {
                return reply(it)
            }
            replyAwait("<@${user}> hat den Draft für sich beendet!")
            addFinished(mem)
            if (current == mem)
                afterPickOfficial()
            save()
        }
    }
}
