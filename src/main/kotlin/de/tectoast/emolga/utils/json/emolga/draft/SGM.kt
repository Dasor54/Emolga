package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.utils.DocEntry
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.coordXMod
import de.tectoast.emolga.utils.records.SorterData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("SGM")
class SGM : League() {
    override val teamsize = 9
    override val pickBuffer = 7

    override val afterTimerSkipMode = AFTER_DRAFT_ORDERED

    override suspend fun RequestBuilder.pickDoc(data: PickData) {
        newSystemPickDoc(data)
        addSingle(data.memIndex.coordXMod("Kader", 2, 'R' - 'B', 3, 19, 9 + data.changedOnTeamsiteIndex), data.pokemon)
        addSingle(data.roundIndex.coordXMod("Draftreihenfolge", 4, 4, 4, 13, 3 + data.indexInRound), data.pokemon)
    }

    @Transient
    override val docEntry = DocEntry.create(this) {
        newSystem(SorterData("Tabelle!C4:K13", newMethod = true, cols = listOf(2, 7, 5))) {
            b.addSingle(gdi.coordXMod("Spielplan (Spoiler)", 2, 'F' - 'B', 3, 9, 7 + index), defaultGameplanString)
        }
    }
}
