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

import java.io.FileWriter
import java.time.Duration
import javafx.beans.property.{ObjectProperty, SimpleObjectProperty}
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.stage.Stage

import info.gianlucacosta.helios.fx.Includes._
import info.gianlucacosta.helios.fx.dialogs.{Alerts, InputDialogs}
import info.gianlucacosta.twobinpack.core.{FrameMode, Problem, ProblemBundle, Solution}
import info.gianlucacosta.twobinpack.io.FileExtensions
import info.gianlucacosta.twobinpack.io.csv.SolutionCsvWriter
import info.gianlucacosta.twobinpack.rendering.frame.Frame
import info.gianlucacosta.twobinpack.rendering.frame.axes.AxesPane
import info.gianlucacosta.twobinpack.rendering.gallery.BlockGalleryPane

import scalafx.Includes._
import scalafx.beans.binding.Bindings
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.paint.Color
import scalafx.stage.FileChooser


private object GameController {
  private val NoTimeLimitText =
    "(no time limit)"
}

/**
  * The controller handling the game
  */
private class GameController {
  var stage: Stage = _


  private val csvSolutionsFileChooser = new FileChooser {
    extensionFilters.setAll(
      new FileChooser.ExtensionFilter("Solution file", s"*${FileExtensions.CsvSolutionFile}")
    )

    title =
      "Save solutions..."
  }


  private val reversedSolutions =
    new SimpleObjectProperty[List[Solution]]


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

  problemOption.onChange {
    bestSolutionOption() =
      None
  }


  private val remainingProblems =
    new SimpleObjectProperty[List[Problem]](List())


  private val frameOption =
    new SimpleObjectProperty[Option[Frame]](None)


  frameOption.onChange {
    frameOption().foreach(frame => {
      val problem =
        problemOption().get

      solutionOption <==
        Bindings.createObjectBinding(
          () => {
            Some(
              Solution(
                problem,
                solverOption(),
                frame.blocks()
              )
            )
          },

          frame.blocks
        )

      targetOption <==
        Bindings.createObjectBinding[Option[Int]](
          () => {
            solutionOption().flatMap(_.target)
          },

          solutionOption
        )


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
            new BlockGalleryPane(
              frame.blockGallery(),
              problem.frameTemplate.colorPalette,
              resolutionSlider.value().toInt
            ).delegate
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

  private[twobingame] val countdownTimerOption =
    new SimpleObjectProperty[Option[CountdownTimer]](None)


  countdownTimerOption.onChange {
    countdownTimerOption() match {

      case Some(secondsCountdownTimer) =>
        timeRemainingLabel.text <==
          Bindings.createStringBinding(
            () => {
              val remainingDuration =
                Duration.ofSeconds(secondsCountdownTimer.value())

              val remainingHours =
                remainingDuration.toHours

              val remainingMinutes =
                remainingDuration.toMinutes % 60

              val remainingSeconds =
                remainingDuration.getSeconds % 60


              if (remainingHours > 0)
                f"${remainingHours}%02d:${remainingMinutes}%02d:${remainingSeconds}%02d"
              else
                f"${remainingMinutes}%02d:${remainingSeconds}%02d"
            },

            secondsCountdownTimer.value
          )


        timeRemainingLabel.textFill <==
          Bindings.createObjectBinding[javafx.scene.paint.Paint](
            () => {
              if (secondsCountdownTimer.value() <= 60)
                Color.Crimson.delegate
              else
                Color.Black.delegate
            },

            secondsCountdownTimer.value
          )


        secondsCountdownTimer.value.onChange {
          if (secondsCountdownTimer.value() == 0) {
            registerSolution(secondsCountdownTimer.problem)
          }
        }


      case None =>
        timeRemainingLabel.textFill.unbind()
        timeRemainingLabel.textFill =
          Color.Black

        timeRemainingLabel.text.unbind()
        timeRemainingLabel.text =
          GameController.NoTimeLimitText
    }

    ()
  }


  private val targetOption =
    new SimpleObjectProperty[Option[Int]](None)


  targetOption.onChange {
    updateBestSolutionOption()
  }


  private def updateBestSolutionOption(): Unit = {
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
        saveSolutionsAndClose()
    }
  }

  private def canRegisterSolution: Boolean = {
    if (bestTargetOption().isDefined)
      true
    else {
      val confirmationOption =
        InputDialogs.askYesNoCancel("You still have not found a valid solution!\nDo you really want to save the current solution?")

      confirmationOption.contains(true)
    }
  }

  private def registerSolution(targetProblem: Problem): Unit = {
    if (problemOption().contains(targetProblem)) {
      countdownTimerOption().foreach(countdownTimer => {
        if (countdownTimer.problem == targetProblem) {
          countdownTimer.stop()
        }
      })


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


  private def saveSolutionsAndClose(): Unit = {
    csvSolutionsFileChooser.initialFileName =
      "Solutions" +
        solverOption()
          .map(solver => s"_${solver}")
          .getOrElse("")

    val solutionsFile =
      csvSolutionsFileChooser.smartSave(stage)

    if (solutionsFile != null) {
      val solutions =
        reversedSolutions().reverse

      try {
        val solutionsWriter =
          new SolutionCsvWriter(new FileWriter(solutionsFile))

        try {
          solutions.foreach(solutionsWriter.writeSolution)

          Alerts.showInfo("Solutions saved successfully.\n\nThank you for playing! ^__^")

          stage.hide()
        } finally {
          solutionsWriter.close()
        }
      } catch {
        case ex: Exception =>
          Alerts.showException(
            ex,
            alertType = AlertType.Warning
          )
      }
    }
  }


  @FXML
  def initialize(): Unit = {
    timeRemainingLabel.text =
      GameController.NoTimeLimitText

    resolutionSlider.min =
      Problem.MinResolution

    resolutionSlider.max =
      Problem.MaxResolution


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


    bestTargetOption <==
      Bindings.createObjectBinding[Option[Int]](
        () => {
          bestSolutionOption().flatMap(bestSolution =>
            bestSolution.target
          )
        },

        bestSolutionOption
      )


    countdownTimerOption <==
      Bindings.createObjectBinding[Option[CountdownTimer]](
        () => {
          problemOption().flatMap(problem => {
            problem.timeLimitInMinutesOption.map(timeLimitInMinutes => {
              val timeLimitInSeconds =
                timeLimitInMinutes * 60

              new CountdownTimer(
                problem,
                timeLimitInSeconds,
                Duration.ofSeconds(1)
              ) {
                start()
              }
            })
          })
        },

        problemOption
      )


    solverLabel.text <==
      Bindings.createStringBinding(
        () => {
          solverOption().getOrElse("(anonymous)")
        },

        solverOption
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

    timeRemainingPromptLabel.textFill <==
      timeRemainingLabel.textFill


    resolutionLabel.text <==
      Bindings.createStringBinding(
        () =>
          s"${resolutionSlider.value().toInt} px",

        resolutionSlider.value
      )


    statusBox.visible <==
      problemOption =!= None

    targetBox.visible <==
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


    gameSplitPane.visible <==
      frameOption =!= None


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
  var timeRemainingPromptLabel: javafx.scene.control.Label = _

  @FXML
  var timeRemainingLabel: javafx.scene.control.Label = _


  @FXML
  var blocksPromptLabel: javafx.scene.control.Label = _

  @FXML
  var blocksLabel: javafx.scene.control.Label = _


  @FXML
  var resolutionSlider: javafx.scene.control.Slider = _

  @FXML
  var resolutionLabel: javafx.scene.control.Label = _


  @FXML
  var targetBox: javafx.scene.layout.VBox = _


  @FXML
  var targetPromptLabel: javafx.scene.control.Label = _

  @FXML
  var targetLabel: javafx.scene.control.Label = _

  @FXML
  var bestTargetLabel: javafx.scene.control.Label = _


  @FXML
  var gameSplitPane: javafx.scene.control.SplitPane = _

  @FXML
  var frameScrollPane: javafx.scene.control.ScrollPane = _

  @FXML
  var galleryScrollPane: javafx.scene.control.ScrollPane = _

  @FXML
  var commitButton: javafx.scene.control.Button = _
}
