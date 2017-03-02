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

package info.gianlucacosta.twobingame

import javafx.stage.Stage

import info.gianlucacosta.helios.apps.{AppInfo, AuroraAppInfo}
import info.gianlucacosta.helios.fx.application.{AppBase, SplashStage}
import info.gianlucacosta.twobingame.io.actors.Actors
import info.gianlucacosta.twobingame.main.MainScene
import info.gianlucacosta.twobinpack.icons.MainIcon
import info.gianlucacosta.twobinpack.twobingame.ArtifactInfo

import scalafx.Includes._
import scalafx.application.Platform

/**
  * Application's entry point
  */
class App extends AppBase(AuroraAppInfo(ArtifactInfo, MainIcon)) {
  override def startup(appInfo: AppInfo, splashStage: SplashStage, primaryStage: Stage): Unit = {
    Actors.start()

    Platform.runLater {
      primaryStage.scene =
        new MainScene(appInfo, primaryStage)
    }


    Platform.runLater {
      primaryStage.sizeToScene()

      primaryStage.title =
        appInfo.name
    }

    Platform.runLater {
      primaryStage.setResizable(false)
      primaryStage.show()

      primaryStage.centerOnScreen()
    }
  }

  override def stop(): Unit = {
    Actors.stop()
  }
}