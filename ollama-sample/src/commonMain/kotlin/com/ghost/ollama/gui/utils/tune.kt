package com.ghost.ollama.gui.utils

import com.ghost.ollama.gui.SessionView
import com.ghost.ollama.gui.ui.components.TuneOptions
import com.ghost.ollama.models.chat.ChatOptions

// Extension function to convert SessionView to TuneOptions
fun SessionView.toTuneOptions(): TuneOptions {
    return TuneOptions(
        seed = this.seed,
        temperature = this.temperature,
        topK = this.topK,
        topP = this.topP,
        minP = this.minP,
        stop = this.stop,
        numCtx = this.numCtx,
        numPredict = this.numPredict,
        format = this.format
    )
}


fun SessionView.toChatOptions(): ChatOptions {
    return ChatOptions(
        seed = this.seed?.toInt(),
        temperature = this.temperature?.toFloat(),
        topK = this.topK?.toInt(),
        topP = this.topP?.toFloat(),
        minP = this.minP?.toFloat(),
        stop = this.stop,
        numCtx = this.numCtx?.toInt(),
        numPredict = this.numPredict?.toInt(),
    )
}



// Optional: Extension function to apply TuneOptions to a SessionView builder/update
fun SessionView.applyTuneOptions(tuneOptions: TuneOptions): SessionView {
    return this.copy(
        seed = tuneOptions.seed ?: this.seed,
        temperature = tuneOptions.temperature ?: this.temperature,
        topK = tuneOptions.topK ?: this.topK,
        topP = tuneOptions.topP ?: this.topP,
        minP = tuneOptions.minP ?: this.minP,
        stop = tuneOptions.stop ?: this.stop,
        numCtx = tuneOptions.numCtx ?: this.numCtx,
        numPredict = tuneOptions.numPredict ?: this.numPredict,
        format = tuneOptions.format ?: this.format
    )
}

// Optional: Function to merge TuneOptions with existing SessionView
fun mergeTuneOptionsWithSession(
    session: SessionView,
    tuneOptions: TuneOptions
): SessionView {
    return SessionView(
        id = session.id,
        title = session.title,
        pinned = session.pinned,
        createdAt = session.createdAt,
        updatedAt = session.updatedAt,
        sessionType = session.sessionType,
        seed = tuneOptions.seed ?: session.seed,
        temperature = tuneOptions.temperature ?: session.temperature,
        topK = tuneOptions.topK ?: session.topK,
        topP = tuneOptions.topP ?: session.topP,
        minP = tuneOptions.minP ?: session.minP,
        stop = tuneOptions.stop ?: session.stop,
        numCtx = tuneOptions.numCtx ?: session.numCtx,
        numPredict = tuneOptions.numPredict ?: session.numPredict,
        format = tuneOptions.format ?: session.format
    )
}

// Optional: Extension function to check if TuneOptions matches SessionView
fun TuneOptions.matchesSession(session: SessionView): Boolean {
    return (this.seed == null || this.seed == session.seed) &&
            (this.temperature == null || this.temperature == session.temperature) &&
            (this.topK == null || this.topK == session.topK) &&
            (this.topP == null || this.topP == session.topP) &&
            (this.minP == null || this.minP == session.minP) &&
            (this.stop == null || this.stop == session.stop) &&
            (this.numCtx == null || this.numCtx == session.numCtx) &&
            (this.numPredict == null || this.numPredict == session.numPredict) &&
            (this.format == null || this.format == session.format)
}

// Optional: Extension function to get only the parameters that differ from SessionView
fun TuneOptions.getDifferingParameters(session: SessionView): Map<String, Pair<Any?, Any?>> {
    val differences = mutableMapOf<String, Pair<Any?, Any?>>()

    if (seed != null && seed != session.seed) {
        differences["seed"] = Pair(session.seed, seed)
    }
    if (temperature != null && temperature != session.temperature) {
        differences["temperature"] = Pair(session.temperature, temperature)
    }
    if (topK != null && topK != session.topK) {
        differences["topK"] = Pair(session.topK, topK)
    }
    if (topP != null && topP != session.topP) {
        differences["topP"] = Pair(session.topP, topP)
    }
    if (minP != null && minP != session.minP) {
        differences["minP"] = Pair(session.minP, minP)
    }
    if (stop != null && stop != session.stop) {
        differences["stop"] = Pair(session.stop, stop)
    }
    if (numCtx != null && numCtx != session.numCtx) {
        differences["numCtx"] = Pair(session.numCtx, numCtx)
    }
    if (numPredict != null && numPredict != session.numPredict) {
        differences["numPredict"] = Pair(session.numPredict, numPredict)
    }
    if (format != null && format != session.format) {
        differences["format"] = Pair(session.format, format)
    }

    return differences
}