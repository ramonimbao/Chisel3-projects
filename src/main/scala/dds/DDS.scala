package dds

import chisel3._
import chisel3.util._
import chisel3.experimental._

class Counter(freq_in: Double, freq_out: Double, width: Int) extends Module {
	val io = IO(new Bundle {
		val out = Output(UInt(width.W))
	})

	def SinTable(amp: Double, n: Int) = {
		val times = (0 until n).map(i => (i * 2 * Math.PI) / (n.toDouble))
		val inits = times.map(t => {
			val v = Math.round(amp *Math.sin(t) + (amp/2))
			if (v < 0) {
				0.U
			} else if (v > Math.pow(2, width)-1) {
				(Math.pow(2, width)-1).toInt.asUInt
			} else {
				v.U
			}
		})
		VecInit(inits)
	}

	val sin_lut = SinTable(amp=Math.pow(2, width-1), n=Math.pow(2, width).toInt)

	val target = (freq_in / (freq_out * Math.pow(2, width))).toInt
	val counter = RegInit(0.U(log2Ceil(target).W))
	val out = RegInit(0.U(width.W))

	when (counter < target.U) {
		counter := counter + 1.U
	} otherwise {
		counter := 0.U
		out := out + 1.U
	}

	io.out := sin_lut(out)
}

class DDS extends RawModule {
	val io = IO(new Bundle {
		val clock = Input(Clock())
		val reset = Input(Bool())
		val out = Output(UInt(8.W))
		val led = Output(UInt(8.W))
	})

	withClockAndReset(io.clock, !io.reset) {
		val count8 = Module(new Counter(50000000, 440, 8))

		io.out := count8.io.out
		io.led := count8.io.out
	}
}

object DDS extends App {
	chisel3.Driver.execute(Array("--target-dir", "output/DDS"), () => new DDS)
}