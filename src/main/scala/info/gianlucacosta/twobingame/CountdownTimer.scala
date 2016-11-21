/*^
  ===========================================================================
  TwoBinGame
  ===========================================================================
  Copyright (C) 2016 Gianluca Costa
  ===========================================================================
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public
  License along with this program.  If not, see
  <http://www.gnu.org/licenses/gpl-3.0.html>.
  ===========================================================================
*/

package info.gianlucacosta.twobingame

import java.time.Duration
import javafx.beans.property.SimpleIntegerProperty

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.ReadOnlyIntegerProperty

/**
  * Simple countdown timer exposing an FX <i>value</i> property,
  * which is decreased at each tick by a daemon thread.
  *
  * The timer must be started via <i>start()</i>, and automatically
  * stops as soon as its value gets 0; it can also be stopped
  * via <i>stop()</i> but, in the current implementation,
  * it cannot be restarted.
  *
  * @param initialValue The inititial value
  * @param interval     The interval between ticks
  */
private class CountdownTimer(initialValue: Int, interval: Duration) {

  /**
    * The internal thread, updating the FX property
    */
  private class CountdownThread extends Thread(
    new Runnable {
      override def run(): Unit = {
        while (!stopped) {
          Platform.runLater {
            _value() -= 1
          }

          if (value() == 0) {
            CountdownTimer.this.stop()
          }

          Thread.sleep(intervalInMillis)
        }
      }
    }
  ) {
    setDaemon(true)
  }


  require(
    initialValue >= 0,
    "The initial value must be >= 0"
  )

  private val intervalInMillis =
    interval.toMillis

  require(
    intervalInMillis >= 0,
    "The interval must be >= 0"
  )


  private val _value =
    new SimpleIntegerProperty(initialValue)

  def value: ReadOnlyIntegerProperty =
    _value


  private var started =
    false

  private var stopped =
    false


  /**
    * Starts the timer after creation.
    * After the first call, its does nothing.
    */
  def start(): Unit = {
    if (!started) {
      started =
        true

      if (value() > 0) {
        new CountdownThread {
          start()
        }
      }
    }
  }


  /**
    * Stops the timer, if it was started and not stopped;
    * otherwise, it does just nothing
    */
  def stop(): Unit = {
    if (started && !stopped) {
      stopped =
        true
    }
  }
}
