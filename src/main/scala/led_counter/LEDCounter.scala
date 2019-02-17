package led_counter

import chisel3._
import chisel3.experimental._

class LEDCounter extends RawModule {
	val io = IO(new Bundle {
		val CLOCK_50 = Input(Clock())
		val KEY = Input(UInt(2.W))
		val LED = Output(UInt(8.W))
	})

	withClockAndReset(io.CLOCK_50, io.KEY(0)) {
		val counter = RegInit(0.U(26.W))
		val LED = RegInit(0.U(8.W))

		when (counter < 50000000.U) {
			counter := counter + 1.U
		} otherwise {
			counter := 0.U
			LED := LED + 1.U
		}

		io.LED := LED
	}
}

object LEDCounter extends App {
	chisel3.Driver.execute(Array("--target-dir", "output/LEDCounter"), () => new LEDCounter)
}