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

import java.io.{BufferedReader, FileReader}
import javafx.stage.Stage

import info.gianlucacosta.helios.apps.AppInfo
import info.gianlucacosta.helios.fx.Includes._
import info.gianlucacosta.helios.fx.dialogs.Alerts
import info.gianlucacosta.helios.fx.dialogs.about.AboutBox
import info.gianlucacosta.twobinpack.core.ProblemBundle
import info.gianlucacosta.twobinpack.io.FileExtensions
import info.gianlucacosta.twobinpack.io.bundle.ProblemBundleReader

import scalafx.Includes._
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Button, Label}
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{BorderPane, VBox}
import scalafx.scene.text.{Font, FontWeight, TextAlignment}
import scalafx.stage.FileChooser

/**
  * The scene in the primary stage
  *
  * @param appInfo      Product information about the app
  * @param primaryStage The primary stage provided by the FX framework
  */
private class MainScene(appInfo: AppInfo, primaryStage: Stage) extends Scene {
  private val gameStage: GameStage =
    new GameStage(appInfo, primaryStage)


  private lazy val aboutBox: AboutBox =
    new AboutBox(appInfo)


  private val problemBundleFileChooser = new FileChooser {
    extensionFilters.setAll(
      new FileChooser.ExtensionFilter("Problem bundle", s"*${FileExtensions.ProblemBundle}")
    )

    title =
      "Start..."
  }


  root =
    new BorderPane {
      style =
        "-fx-background-color: linear-gradient(to bottom right, #feffd6, #f1ffff);"

      padding =
        Insets(30)


      top =
        new Label {
          text =
            appInfo.name

          style =
            "-fx-text-fill: linear-gradient(to bottom, mediumslateblue, cornflowerblue);"

          textAlignment =
            TextAlignment.Center

          alignment =
            Pos.Center

          font =
            Font.font("Arial", FontWeight.Bold, 56)

          margin =
            Insets(20, 0, 0, 0)

          maxWidth =
            Double.MaxValue
        }


      center =
        new VBox {
          padding =
            Insets(40, 20, 20, 20)

          prefWidth =
            500

          prefHeight =
            300


          spacing =
            30

          alignment =
            Pos.Center


          children = List(
            new Button {
              text =
                "Start..."

              prefWidth =
                400

              prefHeight =
                70


              handleEvent(MouseEvent.MouseClicked) {
                (event: MouseEvent) => {
                  start()
                }
              }
            },


            new Button {
              text =
                "Demo problem"

              prefWidth =
                250

              prefHeight =
                45


              handleEvent(MouseEvent.MouseClicked) {
                (event: MouseEvent) => {
                  startDemo()
                  ()
                }
              }
            },


            new Button {
              text =
                "About..."

              prefWidth =
                250

              prefHeight =
                45


              handleEvent(MouseEvent.MouseClicked) {
                (event: MouseEvent) => {
                  aboutBox.showAndWait()
                  ()
                }
              }
            }
          )
        }
    }


  private def start(): Unit = {
    val problemBundleFile =
      problemBundleFileChooser.smartOpen(window())

    if (problemBundleFile != null) {
      try {
        val problemBundleReader =
          new ProblemBundleReader(new BufferedReader(new FileReader(problemBundleFile)))

        try {
          val problemBundle: ProblemBundle =
            problemBundleReader.readProblemBundle()

          gameStage.startGame(
            problemBundle
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


  private def startDemo(): Unit = {
    gameStage.startGame(
      DemoProblemBundle
    )
  }
}
