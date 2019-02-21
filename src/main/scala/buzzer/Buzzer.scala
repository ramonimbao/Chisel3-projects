package buzzer

import chisel3._
import chisel3.util._
import chisel3.experimental._

class SquareGen(in: Double, out: Double) extends Module {
	val io = IO(new Bundle {
		val out = Output(Bool())
	})

	val target = (in / (out * 2)).toInt
	val counter = RegInit(0.U(log2Ceil(target).W))
	val Q = RegInit(false.B)

	when (counter < target.U) {
		counter := counter + 1.U
	} otherwise {
		counter := 0.U
		Q := !Q
	}

	io.out := Q
}

class SinGen(in: Double, out: Double, width: Int) extends Module {
	val io = IO(new Bundle {
		val out = Output(UInt(width.W))
	})

	// Source: https://github.com/freechipsproject/chisel3/wiki/Memories
	def SinTable(amp: Double, n: Int) = {
		val times = (0 until n).map(i => (i * 2 * Math.PI) / (n.toDouble - 1))
		val inits = times.map(t => Math.round(amp * Math.sin(t) + (amp / 2)).asUInt(width.W))
		VecInit(inits)
	}

	val sin_lut = SinTable(amp=1.0, n=2048)

	val target = (in / (out * 2)).toInt
	val counter = RegInit(0.U(log2Ceil(target).W))
	val index = RegInit(0.U(11.W))

	when (counter < target.U) {
		counter := counter + 1.U
	} otherwise {
		counter := 0.U
		index := index + 1.U
	}

	io.out := sin_lut(index)
}

/*
This uses an Arduino Training Shield that has a buzzer connected to Arduino IO/4,
and three buttons, including the reset button (which is why one of the signals below
is inverted)

Also, I finally figured out how to make my own pin assignments in Quartus lol
 */
class Buzzer extends RawModule {
	val io = IO(new Bundle {
		val CLOCK_50 = Input(Clock())
		val KEY = Input(UInt(2.W))
		//val BUZZER = Output(Bool())
		val BUTTON = Input(UInt(3.W))
		val OUT = Output(UInt(8.W))
	})

	withClockAndReset(io.CLOCK_50, !io.KEY(0)) {
		//val C = Module(new SquareGen(50000000, 261.62))
		//val E = Module(new SquareGen(50000000, 329.62))
		//val G = Module(new SquareGen(50000000, 391.99))

		val sin = Module(new SinGen(in=50000000, out=440, width=8))

		//val out = (C.io.out & !io.BUTTON(0)) | (E.io.out & io.BUTTON(1)) | (G.io.out & io.BUTTON(2))

		//io.BUZZER := out
		io.OUT := sin.io.out
	}
}

object Buzzer extends App {
	chisel3.Driver.execute(Array("--target-dir", "output/Buzzer"), () => new Buzzer)
}