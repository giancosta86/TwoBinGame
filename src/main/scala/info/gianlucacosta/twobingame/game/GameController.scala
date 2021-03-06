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

package info.gianlucacosta.twobingame.game

import java.io.FileWriter
import java.time.Duration
import javafx.beans.property.{ObjectProperty, SimpleBooleanProperty, SimpleObjectProperty}
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.stage.Stage

import info.gianlucacosta.helios.Includes._
import info.gianlucacosta.helios.fx.Includes._
import info.gianlucacosta.helios.fx.dialogs.{Alerts, InputDialogs}
import info.gianlucacosta.helios.fx.time.BasicClock
import info.gianlucacosta.twobinmanager.sdk.server.TwoBinManagerServer
import info.gianlucacosta.twobinpack.core.{FrameMode, Problem, ProblemBundle, Solution}
import info.gianlucacosta.twobinpack.io.FileExtensions
import info.gianlucacosta.twobinpack.io.csv.v2.SolutionCsvWriter2
import info.gianlucacosta.twobinpack.rendering.frame.Frame
import info.gianlucacosta.twobinpack.rendering.frame.axes.AxesPane
import info.gianlucacosta.twobinpack.rendering.gallery.BlockGalleryPane

import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser

/**
  * The controller handling the game
  */
private class GameController {
  var stage: Stage = _

  private[twobingame] val clock =
    new BasicClock(Duration.ofSeconds(1))


  private val csvSolutionsFileChooser = new FileChooser {
    extensionFilters.setAll(
      new FileChooser.ExtensionFilter("Solution file", s"*${FileExtensions.CsvSolutionFile_v2}")
    )

    title =
      "Save solutions..."
  }


  var managerServerOption: Option[TwoBinManagerServer] = _


  private val _problemBundle =
    new SimpleObjectProperty[ProblemBundle](null)

  def problemBundle: ObjectProperty[ProblemBundle] =
    _problemBundle

  def problemBundle_=(newValue: ProblemBundle) =
    _problemBundle() =
      newValue

  problemBundle.onChange {
    val problems =
      problemBundle().problems

    require(
      problems.nonEmpty,
      "The problem bundle must not be empty"
    )

    reversedSolutions() =
      List()

    solverOption() =
      InputDialogs.askForString(
        "Leave it empty - or cancel the dialog - to stay anonymous:",
        "",
        "Please, enter your nickname"
      ).flatMap(solver =>
        if (solver.nonEmpty)
          Some(solver)
        else
          None
      )

    problemOption() =
      Some(problems.head)

    remainingProblems() =
      problems.tail
  }


  private val problemOption =
    new SimpleObjectProperty[Option[Problem]](None)


  private val reversedSolutions =
    new SimpleObjectProperty[List[Solution]]


  private val remainingProblems =
    new SimpleObjectProperty[List[Problem]](List())


  private val frameOption =
    new SimpleObjectProperty[Option[Frame]](None)


  frameOption.onChange {
    frameOption().foreach(frame => {
      val problem =
        problemOption().get

      solutionOption() =
        Some(
          new Solution(
            problem,
            solverOption(),
            Some(Duration.ZERO),
            Set()
          )
        )


      frame.blocks.onChange {
        solutionOption() =
          Some(
            new Solution(
              problem,
              solverOption(),
              Some(
                elapsedTime()
              ),
              frame.blocks()
            )
          )
      }

      bestSolutionOption() =
        None


      blocksLabel.text <==
        Bindings.createStringBinding(
          () => {
            val remainingBlocks =
              problem.frameTemplate.blockPool.totalBlockCount -
                frame.blocks().size

            remainingBlocks.toString
          },

          frame.blocks
        )


      galleryScrollPane.content <==
        Bindings.createObjectBinding[Node](
          () => {
            val blockGalleryPane =
              new BlockGalleryPane(
                frame.blockGallery(),
                problem.frameTemplate.colorPalette,
                resolutionSlider.value().toInt
              )

            blockGalleryPane.delegate
          },

          frame.blockGallery,
          resolutionSlider.value
        )
    })
  }


  private val solverOption =
    new SimpleObjectProperty[Option[String]](None)


  private val solutionOption =
    new SimpleObjectProperty[Option[Solution]](None)


  private val bestSolutionOption =
    new SimpleObjectProperty[Option[Solution]](None)


  private val bestTargetOption =
    new SimpleObjectProperty[Option[Int]](None)


  private val elapsedTime =
    new SimpleObjectProperty[Duration](Duration.ZERO)

  private val remainingTimeOption =
    new SimpleObjectProperty[Option[Duration]](None)


  remainingTimeOption.onChange {
    remainingTimeOption().foreach(remainingTime => {
      if (remainingTime.isZero) {
        registerSolution(problemOption().get)
      }
    })
  }


  private val timeExpiring =
    new SimpleBooleanProperty(false)


  private val targetOption =
    new SimpleObjectProperty[Option[Int]](None)

  targetOption.onChange {
    checkForBestSolution()
  }


  private def checkForBestSolution(): Unit = {
    targetOption().foreach(target => {
      if (target < bestTargetOption().getOrElse(Int.MaxValue)) {
        bestSolutionOption() =
          solutionOption()
      }
    })
  }


  @FXML
  def commit(): Unit = {
    problemOption() match {
      case Some(problem) =>
        if (canRegisterSolution) {
          registerSolution(problem)
        }

      case None =>
        if (saveSolutions()) {
          Alerts.showInfo("Solutions saved successfully.\n\nThank you for playing! ^__^")

          stage.hide()
        }
    }
  }

  private def canRegisterSolution: Boolean = {
    if (bestTargetOption().isDefined)
      true
    else {
      val questionPrompt =
        if (remainingProblems().isEmpty)
          "Do you really want to end the test?"
        else
          "Do you really want to skip to the next problem?"

      val confirmationOption =
        InputDialogs.askYesNoCancel(s"You still have not found a valid solution!\n${questionPrompt}")

      confirmationOption.contains(true)
    }
  }

  private def registerSolution(targetProblem: Problem): Unit = {
    if (problemOption().contains(targetProblem)) {
      clock.reset()

      val solution =
        bestSolutionOption().getOrElse(
          solutionOption().get
        )

      reversedSolutions() =
        solution :: reversedSolutions()

      problemOption() =
        remainingProblems().headOption

      if (remainingProblems().nonEmpty) {
        remainingProblems() =
          remainingProblems().tail
      }
    }
  }


  private def saveSolutions(): Boolean = {
    val solutions =
      reversedSolutions().reverse

    managerServerOption match {
      case Some(managerServer) =>
        if (saveSolutionsToServer(solutions))
          true
        else
          saveSolutionsToFile(solutions)

      case None =>
        saveSolutionsToFile(solutions)
    }
  }


  private def saveSolutionsToServer(
                                     solutions: List[Solution]
                                   ): Boolean = {
    val server =
      managerServerOption.get

    try {
      server.uploadSolutions(solutions)
      true
    } catch {
      case ex: Exception =>
        Alerts.showException(ex, alertType = AlertType.Warning)
        false
    }
  }


  private def saveSolutionsToFile(solutions: List[Solution]): Boolean = {
    csvSolutionsFileChooser.initialFileName =
      "Solutions" +
        solverOption()
          .map(solver => s"_${solver}")
          .getOrElse("")

    val solutionsFile =
      csvSolutionsFileChooser.smartSave(stage)

    if (solutionsFile != null) {
      try {
        val solutionsWriter =
          new SolutionCsvWriter2(new FileWriter(solutionsFile))

        try {
          solutions.foreach(solutionsWriter.writeSolution)
          true
        } finally {
          solutionsWriter.close()
        }
      } catch {
        case ex: Exception =>
          Alerts.showException(
            ex,
            alertType = AlertType.Warning
          )
          false
      }
    }
    else
      false
  }

  @FXML
  def initialize(): Unit = {
    resolutionSlider.min =
      Problem.MinResolution

    resolutionSlider.max =
      Problem.MaxResolution


    elapsedTime <==
      Bindings.createObjectBinding(
        () =>
          Duration.ofSeconds(clock.ticks()),

        clock.ticks
      )

    remainingTimeOption <==
      Bindings.createObjectBinding(
        () => {
          problemOption().flatMap(problem =>
            problem.timeLimitOption.map(timeLimit => {
              val remainingTime: Duration =
                timeLimit - elapsedTime()

              remainingTime
            })
          )
        },

        problemOption,
        elapsedTime
      )


    timeExpiring <==
      Bindings.createBooleanBinding(
        () => {
          remainingTimeOption().exists(remainingTime => {
            remainingTime < Duration.ofMinutes(1)
          })
        },

        remainingTimeOption
      )


    frameOption <==
      Bindings.createObjectBinding[Option[Frame]](
        () => {
          problemOption().map(problem => {
            resolutionSlider.value() =
              problem.frameTemplate.resolution

            new Frame(problem.frameTemplate) {
              resolution <==
                resolutionSlider.value
            }
          })
        },

        problemOption
      )


    targetOption <==
      Bindings.createObjectBinding[Option[Int]](
        () => {
          solutionOption().flatMap(_.target)
        },

        solutionOption
      )


    bestTargetOption <==
      Bindings.createObjectBinding[Option[Int]](
        () => {
          bestSolutionOption().flatMap(bestSolution =>
            bestSolution.target
          )
        },

        bestSolutionOption
      )


    solverLabel.text <==
      Bindings.createStringBinding(
        () => {
          solverOption().getOrElse("(anonymous)")
        },

        solverOption
      )


    remainingTimeIndicator.progress <==
      Bindings.createDoubleBinding(
        () => {
          problemOption()
            .flatMap(problem =>
              problem.timeLimitOption.flatMap(timeLimit =>
                remainingTimeOption().map(remainingTime =>
                  remainingTime.getSeconds.toDouble / timeLimit.getSeconds
                )
              )
            )
            .getOrElse(1.0)
        },

        problemOption,
        remainingTimeOption
      )


    remainingTimeIndicator.style <==
      when(timeExpiring) choose {
        "-fx-progress-color: #c11717;"
      } otherwise {
        "-fx-progress-color: #66ae80;"
      }


    remainingTimeLabel.text <==
      Bindings.createStringBinding(
        () => {
          remainingTimeOption()
            .map(remainingTime => {
              remainingTime.digitalFormat
            })
            .getOrElse("(no time limit)")

        },

        remainingTimeOption
      )


    remainingTimeLabel.textFill <==
      Bindings.createObjectBinding[javafx.scene.paint.Paint](
        () => {
          if (timeExpiring())
            Color.Crimson.delegate
          else
            Color.Black.delegate
        },


        timeExpiring
      )


    remainingTimePromptLabel.textFill <==
      remainingTimeLabel.textFill


    remainingTimeBox.managed <==
      remainingTimeOption =!= None


    remainingTimeBox.visible <==
      remainingTimeBox.managed


    elapsedTimeBox.managed <==
      !remainingTimeBox.managed

    elapsedTimeBox.visible <==
      elapsedTimeBox.managed


    elapsedTimeLabel.text <==
      Bindings.createStringBinding(
        () => {
          elapsedTime().digitalFormat
        },

        elapsedTime
      )


    commitButton.text <==
      Bindings.createStringBinding(
        () => {
          remainingProblems() match {
            case Nil =>
              problemOption() match {
                case Some(_) =>
                  "End test"

                case None =>
                  "Save and close..."
              }

            case _ =>
              "Next problem"
          }
        },

        problemOption,
        remainingProblems
      )


    problemLabel.text <==
      Bindings.createStringBinding(
        () => {
          problemOption().map(problem => {
            val problemOrdinal =
              reversedSolutions().size + 1

            val totalProblemsCount =
              _problemBundle().problems.size

            s"${problemOrdinal} / ${totalProblemsCount}: ${problem.name}"
          })
            .getOrElse("")
        },

        problemOption,
        reversedSolutions
      )


    frameModeLabel.text <==
      Bindings.createStringBinding(
        () => {
          frameOption().map(frame => {
            frame.frameTemplate.frameMode.name
          }).getOrElse("")

        },
        frameOption
      )


    rotationLabel.text <==
      Bindings.createStringBinding(
        () => {
          problemOption().map(problem => {
            if (problem.frameTemplate.blockPool.canRotateBlocks)
              "Yes"
            else
              "No"
          }).getOrElse("")
        },
        problemOption
      )


    blocksPromptLabel.text <==
      Bindings.createStringBinding(
        () => {
          problemOption().map(problem => {
            problem.frameTemplate.frameMode match {
              case FrameMode.Knapsack =>
                "Available blocks:"

              case FrameMode.Strip =>
                "Blocks to insert:"
            }
          }).getOrElse("")
        },
        problemOption
      )


    resolutionLabel.text <==
      Bindings.createStringBinding(
        () =>
          s"${resolutionSlider.value().toInt}",

        resolutionSlider.value
      )


    statusBox.visible <==
      problemOption =!= None

    sideBox.visible <==
      problemOption =!= None


    targetLabel.text <==
      Bindings.createStringBinding(
        () =>
          Solution.formatTarget(targetOption()),

        targetOption
      )

    targetLabel.textFill <==
      Bindings.createObjectBinding[javafx.scene.paint.Paint](
        () => {
          targetOption().flatMap(target => {
            bestTargetOption().flatMap(bestTarget =>
              if (target == bestTarget)
                Some(Color.ForestGreen.delegate)
              else
                None
            )
          })
            .getOrElse(Color.Black.delegate)
        },

        targetOption,
        bestTargetOption
      )

    targetPromptLabel.textFill <==
      targetLabel.textFill


    bestTargetLabel.text <==
      Bindings.createStringBinding(
        () =>
          Solution.formatTarget(bestTargetOption()),

        bestTargetOption
      )


    frameScrollPane.visible <==
      frameOption =!= None


    galleryScrollPane.visible <==
      frameScrollPane.visible


    frameScrollPane.content <==
      Bindings.createObjectBinding[Node](
        () => {
          frameOption().map(frame => {
            new AxesPane(
              frame
            ).delegate
          }).orNull
        },

        frameOption
      )


    frameScrollPane.hbarPolicy <==
      Bindings.createObjectBinding[ScrollBarPolicy](
        () => {
          frameOption().map(frame => {
            frame.frameTemplate.frameMode match {
              case FrameMode.Knapsack =>
                ScrollBarPolicy.AS_NEEDED

              case FrameMode.Strip =>
                ScrollBarPolicy.ALWAYS
            }
          }).getOrElse(ScrollBarPolicy.AS_NEEDED)
        },

        frameOption
      )

    clock.start()
  }


  @FXML
  var statusBox: javafx.scene.layout.FlowPane = _


  @FXML
  var solverLabel: javafx.scene.control.Label = _


  @FXML
  var problemPromptLabel: javafx.scene.control.Label = _


  @FXML
  var problemLabel: javafx.scene.control.Label = _


  @FXML
  var frameModePromptLabel: javafx.scene.control.Label = _

  @FXML
  var frameModeLabel: javafx.scene.control.Label = _

  @FXML
  var rotationLabel: javafx.scene.control.Label = _


  @FXML
  var blocksPromptLabel: javafx.scene.control.Label = _

  @FXML
  var blocksLabel: javafx.scene.control.Label = _


  @FXML
  var resolutionSlider: javafx.scene.control.Slider = _

  @FXML
  var resolutionLabel: javafx.scene.control.Label = _


  @FXML
  var sideBox: javafx.scene.layout.VBox = _


  @FXML
  var targetPromptLabel: javafx.scene.control.Label = _

  @FXML
  var targetLabel: javafx.scene.control.Label = _

  @FXML
  var bestTargetLabel: javafx.scene.control.Label = _

  @FXML
  var remainingTimeBox: javafx.scene.layout.VBox = _

  @FXML
  var remainingTimeIndicator: javafx.scene.control.ProgressIndicator = _

  @FXML
  var remainingTimePromptLabel: javafx.scene.control.Label = _

  @FXML
  var remainingTimeLabel: javafx.scene.control.Label = _


  @FXML
  var elapsedTimeBox: javafx.scene.layout.VBox = _

  @FXML
  var elapsedTimePromptLabel: javafx.scene.control.Label = _

  @FXML
  var elapsedTimeLabel: javafx.scene.control.Label = _


  @FXML
  var gameSplitPane: javafx.scene.control.SplitPane = _

  @FXML
  var frameScrollPane: javafx.scene.control.ScrollPane = _

  @FXML
  var galleryScrollPane: javafx.scene.control.ScrollPane = _

  @FXML
  var commitButton: javafx.scene.control.Button = _
}
