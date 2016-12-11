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

package info.gianlucacosta.helios.fx.time

import java.time.Duration
import javafx.beans.property.SimpleIntegerProperty

import scala.annotation.tailrec
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.ReadOnlyIntegerProperty

/**
  * Simple clock exposing an FX <i>ticks</i> property,
  * which is increased at each tick by a daemon thread.
  *
  * The timer must be started via <i>start()</i> and
  * should then be stopped via <i>stop()</i> when it is no
  * more needed.
  *
  * To reset the ticks to 0, one should call the reset() button.
  *
  * @param interval The interval between ticks
  */
class Clock(interval: Duration) {

  /**
    * The internal thread, updating the FX property
    */
  private class TickThread extends Thread(
    new Runnable {
      @tailrec
      override def run(): Unit = {
        if (!stopped) {
          Thread.sleep(intervalInMillis)

          Platform.runLater {
            _ticks() += 1
          }

          run()
        }
      }
    }
  ) {
    setDaemon(true)
  }


  private val intervalInMillis =
    interval.toMillis

  require(
    intervalInMillis > 0,
    "The interval must be > 0"
  )


  private val _ticks =
    new SimpleIntegerProperty(0)

  def ticks: ReadOnlyIntegerProperty =
    _ticks


  private var started =
    false

  @volatile
  private var stopped =
    false


  /**
    * Starts the clock after creation.
    * After the first call, its does nothing.
    */
  def start(): Unit = {
    if (!started) {
      started =
        true

      new TickThread {
        start()
      }
    }
  }


  /**
    * Stops the clock, if it was started and not stopped;
    * otherwise, it does just nothing
    */
  def stop(): Unit = {
    stopped =
      true
  }


  /**
    * Resets the ticks without stopping the clock
    */
  def reset(): Unit = {
    _ticks() = 0
  }
}

