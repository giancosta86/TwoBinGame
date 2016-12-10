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

import info.gianlucacosta.twobinpack.core._

/**
  * Example problem bundle shown in the "Demo" game feature
  */
private object DemoProblemBundle extends ProblemBundle(List(
  Problem(
    FrameTemplate(
      FrameDimension(
        3,
        3
      ),

      FrameMode.Strip,

      BlockPool.create(
        canRotateBlocks = true,

        BlockDimension(
          3,
          1
        ) -> 2,

        BlockDimension(
          2,
          2
        ) -> 2
      ),

      FrameTemplate.SuggestedBlockColorsPool,

      40
    ),

    Some(
      Duration.ofMinutes(2)
    ),

    "Example problem"
  )
))
