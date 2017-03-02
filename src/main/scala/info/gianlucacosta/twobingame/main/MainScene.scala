/*^
  ===========================================================================
  TwoBinGame
  ===========================================================================
  Copyright (C) 2016-2017 Gianluca Costa
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

package info.gianlucacosta.twobingame.main

import javafx.scene.layout.BorderPane
import javafx.stage.Stage

import info.gianlucacosta.helios.apps.AppInfo
import info.gianlucacosta.helios.fx.scene.fxml.FxmlScene

class MainScene(
                 appInfo: AppInfo,
                 primaryStage: Stage
               ) extends FxmlScene[MainController, BorderPane](
  classOf[MainController]
) {
  override protected def preInitialize(): Unit = {
    controller.appInfo =
      appInfo

    controller.stage =
      primaryStage
  }
}
