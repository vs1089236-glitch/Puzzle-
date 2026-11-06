package com.example.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.abs

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code encoder (Byte mode, Error Correction Level M, Versions 1..7)
 * generating standard UPI payment QR matrices (`upi://pay?pa=...&pn=...&am=...&cu=INR&tn=...`).
 */
data class QrMatrix(
    val size: Int,
    val modules: BooleanArray
) {
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]
}

object QrCodeGenerator {

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int,
        val alignmentCenters: IntArray
    )

    // ISO/IEC 18004 Table 9 specs for Error Correction Level M (Versions 1..7)
    private val VERSION_SPECS = listOf(
        VersionSpec(1, 21, 16, 10, 1, intArrayOf()),
        VersionSpec(2, 25, 28, 16, 1, intArrayOf(6, 18)),
        VersionSpec(3, 29, 44, 26, 1, intArrayOf(6, 22)),
        VersionSpec(4, 33, 64, 18, 2, intArrayOf(6, 26)),
        VersionSpec(5, 37, 86, 24, 2, intArrayOf(6, 30)),
        VersionSpec(6, 41, 108, 16, 4, intArrayOf(6, 34)),
        VersionSpec(7, 45, 124, 18, 4, intArrayOf(6, 22, 38))
    )

    fun buildUpiPaymentUri(
        upiId: String,
        payeeName: String,
        amount: Double,
        note: String
    ): String {
        val cleanUpi = upiId.trim().ifBlank { "vishalkumar90@upi" }
        val encodedName = URLEncoder.encode(payeeName.trim().take(24), StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        val formattedAmount = String.format(java.util.Locale.US, "%.2f", amount)
        val encodedNote = URLEncoder.encode(note.trim().take(20), StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "upi://pay?pa=$cleanUpi&pn=$encodedName&am=$formattedAmount&cu=INR&tn=$encodedNote"
    }

    fun encodeUpiQr(
        upiId: String,
        payeeName: String,
        amount: Double,
        note: String
    ): QrMatrix {
        val uri = buildUpiPaymentUri(upiId, payeeName, amount, note)
        return encodeText(uri)
    }

    fun encodeText(text: String): QrMatrix {
        val rawBytes = text.toByteArray(StandardCharsets.UTF_8)
        // Byte mode uses 4 bits mode indicator + 8 bits character count = 12 bits overhead (~2 codewords)
        val spec = VERSION_SPECS.firstOrNull { rawBytes.size + 2 <= it.totalDataCodewords }
            ?: VERSION_SPECS.last()
        val payloadBytes = if (rawBytes.size + 2 > spec.totalDataCodewords) {
            rawBytes.copyOf(spec.totalDataCodewords - 2)
        } else {
            rawBytes
        }

        // 1. Build data bit stream
        val bits = ArrayList<Int>(spec.totalDataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in (count - 1) downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        appendBits(0b0100, 4) // Byte mode indicator
        appendBits(payloadBytes.size, 8) // Character count (8-bit for versions 1..9)
        for (b in payloadBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        val maxBits = spec.totalDataCodewords * 8
        val terminatorLen = minOf(4, maxBits - bits.size)
        repeat(terminatorLen) { bits.add(0) }
        while (bits.size % 8 != 0) {
            bits.add(0)
        }

        val dataCodewords = IntArray(spec.totalDataCodewords)
        var cwIdx = 0
        for (i in bits.indices step 8) {
            var byteVal = 0
            for (b in 0 until 8) {
                byteVal = (byteVal shl 1) or bits[i + b]
            }
            dataCodewords[cwIdx++] = byteVal
        }
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (cwIdx < spec.totalDataCodewords) {
            dataCodewords[cwIdx++] = padBytes[padIdx % 2]
            padIdx++
        }

        // 2. Split into blocks & compute Reed-Solomon EC codewords
        val blockSize = spec.totalDataCodewords / spec.numBlocks
        val dataBlocks = Array(spec.numBlocks) { b ->
            dataCodewords.copyOfRange(b * blockSize, (b + 1) * blockSize)
        }
        val ecBlocks = Array(spec.numBlocks) { b ->
            computeReedSolomon(dataBlocks[b], spec.ecCodewordsPerBlock)
        }

        // 3. Interleave data & EC codewords
        val totalCodewords = spec.totalDataCodewords + spec.numBlocks * spec.ecCodewordsPerBlock
        val finalStream = IntArray(totalCodewords)
        var outIdx = 0
        for (i in 0 until blockSize) {
            for (b in 0 until spec.numBlocks) {
                finalStream[outIdx++] = dataBlocks[b][i]
            }
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (b in 0 until spec.numBlocks) {
                finalStream[outIdx++] = ecBlocks[b][i]
            }
        }

        // 4. Construct function patterns
        val size = spec.size
        val isFunction = BooleanArray(size * size)
        val baseModules = BooleanArray(size * size)

        fun setFunctionModule(x: Int, y: Int, dark: Boolean) {
            if (x in 0 until size && y in 0 until size) {
                isFunction[y * size + x] = true
                baseModules[y * size + x] = dark
            }
        }

        fun drawFinderPattern(cx: Int, cy: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val dist = maxOf(abs(dx), abs(dy))
                    val x = cx + dx
                    val y = cy + dy
                    if (x in 0 until size && y in 0 until size) {
                        val dark = dist != 2 && dist != 4
                        setFunctionModule(x, y, dark)
                    }
                }
            }
        }

        drawFinderPattern(3, 3)
        drawFinderPattern(size - 4, 3)
        drawFinderPattern(3, size - 4)

        // Alignment patterns
        val centers = spec.alignmentCenters
        for (cy in centers) {
            for (cx in centers) {
                val overlapsFinder =
                    (cx <= 8 && cy <= 8) ||
                    (cx >= size - 8 && cy <= 8) ||
                    (cx <= 8 && cy >= size - 8)
                if (!overlapsFinder) {
                    for (dy in -2..2) {
                        for (dx in -2..2) {
                            val dist = maxOf(abs(dx), abs(dy))
                            setFunctionModule(cx + dx, cy + dy, dist != 1)
                        }
                    }
                }
            }
        }

        // Timing patterns
        for (i in 8 until size - 8) {
            val dark = (i % 2) == 0
            setFunctionModule(i, 6, dark)
            setFunctionModule(6, i, dark)
        }

        // Dark module
        setFunctionModule(8, 4 * spec.version + 9, true)

        // Reserve format info areas
        for (i in 0..8) {
            if (i != 6) {
                setFunctionModule(8, i, false)
                setFunctionModule(i, 8, false)
            }
        }
        for (i in 0 until 8) {
            setFunctionModule(size - 1 - i, 8, false)
            if (i < 7) {
                setFunctionModule(8, size - 1 - i, false)
            }
        }

        // Version info (required for Version >= 7)
        if (spec.version >= 7) {
            val versionBits = computeVersionInfo(spec.version)
            for (i in 0 until 18) {
                val dark = ((versionBits ushr i) and 1) != 0
                val a = size - 11 + (i % 3)
                val b = i / 3
                setFunctionModule(a, b, dark)
                setFunctionModule(b, a, dark)
            }
        }

        // 5. Place data bits in zigzag order
        var bitIdx = 0
        val totalBits = finalStream.size * 8
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                val upward = ((right + 1) and 2) == 0
                val y = if (upward) (size - 1 - vert) else vert
                for (j in 0..1) {
                    val x = right - j
                    if (!isFunction[y * size + x]) {
                        var dark = false
                        if (bitIdx < totalBits) {
                            val cw = finalStream[bitIdx ushr 3]
                            dark = ((cw ushr (7 - (bitIdx and 7))) and 1) != 0
                            bitIdx++
                        }
                        baseModules[y * size + x] = dark
                    }
                }
            }
            right -= 2
        }

        // 6. Evaluate all 8 masks and pick lowest penalty
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        var bestModules = baseModules

        for (mask in 0..7) {
            val candidate = baseModules.copyOf()
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (!isFunction[y * size + x] && shouldInvert(mask, x, y)) {
                        candidate[y * size + x] = !candidate[y * size + x]
                    }
                }
            }
            applyFormatInfo(candidate, size, mask)
            val penalty = computePenalty(candidate, size)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
                bestModules = candidate
            }
        }

        return QrMatrix(size = size, modules = bestModules)
    }

    private fun shouldInvert(mask: Int, x: Int, y: Int): Boolean = when (mask) {
        0 -> (x + y) % 2 == 0
        1 -> y % 2 == 0
        2 -> x % 3 == 0
        3 -> (x + y) % 3 == 0
        4 -> ((y / 2) + (x / 3)) % 2 == 0
        5 -> ((x * y) % 2) + ((x * y) % 3) == 0
        6 -> (((x * y) % 2) + ((x * y) % 3)) % 2 == 0
        7 -> (((x + y) % 2) + ((x * y) % 3)) % 2 == 0
        else -> false
    }

    private fun applyFormatInfo(modules: BooleanArray, size: Int, mask: Int) {
        // EC Level M = 0b00
        val data = (0b00 shl 3) or mask
        var rem = data shl 10
        for (bit in 14 downTo 10) {
            if (((rem ushr bit) and 1) != 0) {
                rem = rem xor (0x537 shl (bit - 10))
            }
        }
        val formatBits = ((data shl 10) or rem) xor 0x5412

        // Copy 1: top-left around finder
        for (i in 0..5) {
            val dark = ((formatBits ushr i) and 1) != 0
            modules[i * size + 8] = dark
        }
        modules[7 * size + 8] = ((formatBits ushr 6) and 1) != 0
        modules[8 * size + 8] = ((formatBits ushr 7) and 1) != 0
        modules[8 * size + 7] = ((formatBits ushr 8) and 1) != 0
        for (i in 9..14) {
            val dark = ((formatBits ushr i) and 1) != 0
            modules[8 * size + (14 - i)] = dark
        }

        // Copy 2: bottom-left & top-right
        for (i in 0..7) {
            val dark = ((formatBits ushr i) and 1) != 0
            modules[8 * size + (size - 1 - i)] = dark
        }
        for (i in 8..14) {
            val dark = ((formatBits ushr i) and 1) != 0
            modules[(size - 15 + i) * size + 8] = dark
        }
    }

    private fun computeVersionInfo(version: Int): Int {
        var rem = version shl 12
        for (bit in 17 downTo 12) {
            if (((rem ushr bit) and 1) != 0) {
                rem = rem xor (0x1F25 shl (bit - 12))
            }
        }
        return (version shl 12) or rem
    }

    private fun computeReedSolomon(data: IntArray, ecCount: Int): IntArray {
        var gen = intArrayOf(1)
        var alphaPow = 1
        for (i in 0 until ecCount) {
            val next = IntArray(gen.size + 1)
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], alphaPow)
            }
            gen = next
            alphaPow = gfMul(alphaPow, 2)
        }

        val rem = IntArray(ecCount)
        for (b in data) {
            val factor = b xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecCount - 1)
            rem[ecCount - 1] = 0
            for (j in 0 until ecCount) {
                rem[j] = rem[j] xor gfMul(gen[j + 1], factor)
            }
        }
        return rem
    }

    private fun gfMul(a: Int, b: Int): Int {
        var x = a
        var y = b
        var res = 0
        while (y > 0) {
            if ((y and 1) != 0) res = res xor x
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11D
            y = y ushr 1
        }
        return res
    }

    private fun computePenalty(modules: BooleanArray, size: Int): Int {
        var penalty = 0
        // Rule 1: adjacent same-color modules in row/col
        for (y in 0 until size) {
            var runColor = modules[y * size]
            var runLen = 1
            for (x in 1 until size) {
                val c = modules[y * size + x]
                if (c == runColor) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runColor = c
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        for (x in 0 until size) {
            var runColor = modules[x]
            var runLen = 1
            for (y in 1 until size) {
                val c = modules[y * size + x]
                if (c == runColor) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runColor = c
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        // Rule 2: 2x2 blocks
        for (y in 0 until size - 1) {
            for (x in 0 until size - 1) {
                val c = modules[y * size + x]
                if (c == modules[y * size + x + 1] &&
                    c == modules[(y + 1) * size + x] &&
                    c == modules[(y + 1) * size + x + 1]
                ) {
                    penalty += 3
                }
            }
        }
        return penalty
    }
}
