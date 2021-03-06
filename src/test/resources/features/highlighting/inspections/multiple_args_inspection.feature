Feature: Inspection for multiple arguments in various sections
  Scenario Outline: Multiple arguments in subworkflow section
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    subworkflow NAME:
        <section>: "a", "b", "c"
    """
    And Section Multiple Args inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for 'subworkflow' section.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for 'subworkflow' section.
    """
    When I check highlighting errors
    Examples:
    | section    |
    | workdir    |
    | snakefile  |
    | configfile |

  Scenario Outline: Multiple arguments in execution sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
        <section>: "a", "b", "c"
    """
    And Section Multiple Args inspection is enabled
    Then I expect inspection error on <"b"> with message
    """
    Only one argument is allowed for '<section>' section.
    """
    And I expect inspection error on <"c"> with message
    """
    Only one argument is allowed for '<section>' section.
    """
    When I check highlighting errors
    Examples:
      | section     |
      | shell       |
      | script      |
      | wrapper     |
      | cwl         |
      | conda       |
      | singularity |
      | priority    |
      | version     |
      | group       |
      | message     |
      | benchmark   |
      | threads     |
      | shadow      |