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

import java.io.{BufferedReader, FileReader}
import javafx.fxml.FXML
import javafx.stage.Stage

import info.gianlucacosta.helios.apps.AppInfo
import info.gianlucacosta.helios.fx.Includes._
import info.gianlucacosta.helios.fx.dialogs.about.AboutBox
import info.gianlucacosta.helios.fx.dialogs.{Alerts, InputDialogs}
import info.gianlucacosta.twobingame.DemoProblemBundle
import info.gianlucacosta.twobingame.game.GameStage
import info.gianlucacosta.twobingame.io.actors.Actors
import info.gianlucacosta.twobinmanager.sdk.server.TwoBinManagerServer
import info.gianlucacosta.twobinpack.core.ProblemBundle
import info.gianlucacosta.twobinpack.io.FileExtensions
import info.gianlucacosta.twobinpack.io.bundle.ProblemBundleReader

import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.FileChooser


class MainController {
  var appInfo: AppInfo = _
  var stage: Stage = _

  private var gameStage: GameStage = _


  private lazy val aboutBox: AboutBox =
    new AboutBox(appInfo)


  private val problemBundleFileChooser = new FileChooser {
    extensionFilters.setAll(
      new FileChooser.ExtensionFilter("Problem bundle", s"*${FileExtensions.ProblemBundle_v2}")
    )

    title =
      "Start..."
  }


  @FXML
  def initialize(): Unit = {
    gameStage =
      new GameStage(appInfo, stage)

    titleLabel.text =
      appInfo.name
  }

  @FXML
  def playFromFile(): Unit = {
    val problemBundleFile =
      problemBundleFileChooser.smartOpen(stage)

    if (problemBundleFile != null) {
      try {
        val problemBundleReader =
          new ProblemBundleReader(new BufferedReader(new FileReader(problemBundleFile)))

        try {
          val problemBundle: ProblemBundle =
            problemBundleReader.readProblemBundle()

          gameStage.startGame(
            problemBundle,
            None
          )
        } finally {
          problemBundleReader.close()
        }
      } catch {
        case ex: Exception =>
          Alerts.showException(ex, alertType = AlertType.Warning)
      }
    }
  }


  @FXML
  def playFromServer(): Unit = {
    val serverHeader =
      "Enter server coordinates"

    val serverAddressOption =
      InputDialogs.askForString(
        message = "Address:",
        header = serverHeader,
        initialValue = TwoBinManagerServer.defaultConnectionParams.address
      )


    serverAddressOption.foreach(serverAddress => {
      val serverPortOption =
        InputDialogs.askForLong(
          message = "Port:",
          header = serverHeader,
          initialValue = TwoBinManagerServer.defaultConnectionParams.port
        ).map(_.toInt)


      serverPortOption.foreach(serverPort => {
        try {
          val connectionParams =
            TwoBinManagerServer.ConnectionParams(
              serverAddress,
              serverPort
            )

          val server =
            new TwoBinManagerServer(
              Actors.actorSystem,
              connectionParams
            )

          val problemBundle =
            server.requestProblemBundle()

          gameStage.startGame(
            problemBundle,
            Some(server)
          )
        } catch {
          case ex: Exception =>
            Alerts.showException(ex, alertType = AlertType.Warning)
        }
      })
    })
  }


  @FXML
  def startDemo(): Unit = {
    gameStage.startGame(
      DemoProblemBundle,
      None
    )
  }


  @FXML
  def showAboutBox(): Unit = {
    aboutBox.showAndWait()
  }

  @FXML
  var titleLabel: javafx.scene.control.Label = _
}
