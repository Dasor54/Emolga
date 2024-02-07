package de.tectoast.emolga.features.flo


import de.tectoast.emolga.features.*
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions

object PrivCommand : CommandFeature<PrivCommand.Args>(::Args, CommandSpec("priv", "Führt einen privaten Command aus")) {
    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var cmd by fromList("cmd", "cmd", privCommands.keys.toList())
        var arguments by string("args", "args") {
            default = ""
        }
    }

    private val privCommands by lazy {
        PrivateCommands::class.declaredMemberFunctions.filter { it.returnType.classifier == Unit::class }
            .associateBy { it.name }
    }

    context(InteractionData) override suspend fun exec(e: Args) = slashEvent {
        privCommands[e.cmd]?.let { method ->
            if (method.parameters.run { isEmpty() || size == 1 }) method.callSuspend(PrivateCommands)
            else method.callSuspend(
                PrivateCommands, self, PrivateData(this)
            )
            if (!isAcknowledged)
                reply("Command ausgeführt!", ephemeral = true)
        } ?: reply("Command nicht gefunden!", ephemeral = true)
    }
}
