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

import javafx.scene.layout.BorderPane

import info.gianlucacosta.helios.apps.AppInfo
import info.gianlucacosta.helios.fx.Includes._
import info.gianlucacosta.helios.fx.dialogs.InputDialogs
import info.gianlucacosta.helios.fx.scene.fxml.FxmlScene
import info.gianlucacosta.helios.fx.stage.StackedStage
import info.gianlucacosta.twobinpack.core.ProblemBundle

import scalafx.Includes._
import scalafx.stage.WindowEvent

/**
  * Stage where the game actually takes place
  *
  * @param appInfo
  * @param previousStage
  */
private class GameStage(
                         appInfo: AppInfo,
                         val previousStage: javafx.stage.Stage
                       ) extends StackedStage {

  title =
    appInfo.name

  this.setMainIcon(appInfo)

  private var currentController: GameController = _

  /**
    * Starts a new game
    *
    * @param problemBundle The problem bundle
    */
  def startGame(problemBundle: ProblemBundle) = {
    val fxmlScene =
      new FxmlScene[GameController, BorderPane](classOf[GameController]) {
        override protected def preInitialize(): Unit = {
          currentController =
            controller

          controller.stage =
            GameStage.this

          controller.problemBundle =
            problemBundle
        }
      }


    scene =
      fxmlScene

    maximized =
      true

    show()
  }


  handleEvent(WindowEvent.WindowCloseRequest) {
    (event: WindowEvent) => {
      if (!canClose) {
        event.consume()
      }
    }
  }


  handleEvent(WindowEvent.WindowHidden) {
    (event: WindowEvent) => {
      currentController.clock.stop()

      event.consume()
    }
  }


  private def canClose: Boolean =
    InputDialogs.askYesNoCancel(
      "Return to the main screen without saving?"
    ).contains(true)
}
