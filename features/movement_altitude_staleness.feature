Feature: A session stops asserting a climb the sensor never actually measured

  Context:
  - Problem: the second field calibration measured a real trace where the altitude froze at exactly
    206.3 m for 385 straight seconds (5 distinct values across 412 readings) while the receiver kept
    declaring 1.67-1.80 m of vertical accuracy the whole time and never flagged anything wrong. The
    climb that happened during that stretch is simply not in the data, yet the session still reports
    a desnivel with the same confidence as one measured from a healthy signal. The cheap escape route
    - discarding readings once the receiver's own vertical accuracy gets worse - was ruled out by that
    same trace: the accuracy never moved.
  - Savings: a desnivel that can be trusted, or an honest "-" instead of a number nobody can check
    against reality.
  - Why: desnivel is the one of the eight figures this pilar reports that is already known to be
    capable of lying.

  As someone who recorded a session where the receiver's altitude got stuck
  I want the app to say it does not know the climb instead of asserting one
  So that a fabricated desnivel never gets the same confidence as a measured one

  Note: this slice refines the escape hatch away from what was first proposed. Counting how many
  consecutive readings repeat the same value depends on the sampling interval, which is not constant;
  what actually matters is how long the value went unchanged, because that is what is directly
  comparable to the 385 s measured in the field. The threshold below (60 s) is a conservative starting
  point - well under the measured incident, with no real trace on record showing a short, legitimate
  repeat - and is flagged here as the number to revisit once another field trace is captured.

  Note: "unchanged" means the exact same floating-point value, never a close one. A real reading never
  repeats to the centimetre; only a stuck sensor does.

  Note: staleness silences the whole session's altitude figures, not just the frozen stretch. A
  session that spent a meaningful stretch with a stuck sensor cannot honestly assert a complete climb
  for the outing, even where the rest of its altitude looks fine.

  @slice-1
  Scenario Outline: A short hold in altitude still counts as real terrain
    Given a session sampled every 2 seconds where the altitude holds at 206.3 m for <held_seconds> seconds before climbing to 217.1 m
    Then the session's ascent <verdict>

    Examples:
      | held_seconds | verdict                                  |
      | 4            | is measured as 10.8 m                    |
      | 58           | is measured as 10.8 m                    |

  @slice-1
  Scenario Outline: A hold that reaches the staleness threshold silences the session's climb
    Given a session sampled every 2 seconds where the altitude holds at 206.3 m for <held_seconds> seconds before climbing to 217.1 m
    Then the session's ascent <verdict>

    Examples:
      | held_seconds | verdict                                  |
      | 60           | reads as unmeasured                      |
      | 385          | reads as unmeasured                      |

  @slice-1
  Scenario: A long freeze silences the whole session, not only the frozen stretch
    Given a session recorded like the field trace: altitude varying normally, then holding at 206.3 m for 385 seconds, then dropping to a real 206.3 m to 217.1 m change and varying normally again
    Then the session's maximum altitude, minimum altitude, ascent, descent, average slope, maximum slope and vertical speed all read as unmeasured
    And the session's distance, moving time and pace are unaffected

  @slice-1
  Scenario: A session with no stale stretch keeps behaving exactly as before this slice
    Given a session where the altitude changes on every single reading
    Then the session's ascent and descent are the same figures this use case already reported

  @slice-1
  Scenario: A session that never reported altitude is unaffected by staleness
    Given a session where the receiver never reported an altitude
    Then the session's altitude figures read as unmeasured, exactly as they did before this slice
