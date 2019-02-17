package buzzer

import chisel3._
import chisel3.util._
import chisel3.experimental._

class FreqGen(in: Double, out: Double) extends Module {
	val io = IO(new Bundle {
		val OutputFrequency = Output(Bool())
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

	io.OutputFrequency := Q
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
		val BUZZER = Output(Bool())
		val BUTTON = Input(UInt(3.W))
	})

	withClockAndReset(io.CLOCK_50, !io.KEY(0)) {
		val C = Module(new FreqGen(50000000, 261.62))
		val E = Module(new FreqGen(50000000, 329.62))
		val G = Module(new FreqGen(50000000, 391.99))

		val out = (C.io.OutputFrequency & !io.BUTTON(0)) | (E.io.OutputFrequency & io.BUTTON(1)) | (G.io.OutputFrequency & io.BUTTON(2))

		io.BUZZER := out
	}
}

object Buzzer extends App {
	chisel3.Driver.execute(Array("--target-dir", "output/Buzzer"), () => new Buzzer)
}