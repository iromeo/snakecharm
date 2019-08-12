Feature: Tests on snakemake string language injection

  Scenario Outline: Injection for different quotes
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: <quote>{input}<quote>
    """
    When I put the caret after input
    Then I expect language injection on "{input}"
    Examples:
      | quote |
      | "     |
      | '     |
      | """   |

  Scenario Outline: Ordinary injection for rule/checkpoint
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section> NAME:
      shell: "{input}"
    """
    When I put the caret after input
    Then I expect language injection on "{input}"
    Examples:
      | section      |
      | rule         |
      | checkpoint   |

  Scenario: No injection in fstrings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: f"{input}"
    """
    When I put the caret after input
    Then I expect no language injection

  Scenario: Injection in split string literal
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      shell: "somecommand" "{wildcards.group}"  f'{10}' "{output}"
    """
    When I put the caret after somecommand
    Then I expect language injection on "somecommand{wildcards.group}{output}"

  Scenario: No injection for ordinary string literals
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    str = "NAME"
    """
    When I put the caret after NAME
    Then I expect no language injection

  Scenario: No injection in ordinary function calls
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    def f(s)
      return s

    rule NAME:
      output: f("{foo}")
    """
    When I put the caret after foo
    Then I expect no language injection

  Scenario Outline: No injection in strings without braces
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: "<content>"#here
    """
    When I put the caret at "#here
    Then I expect no language injection
    Examples:
    | content |
    | text    |
    |         |
    | text {  |
    | text }  |

  Scenario Outline: No injection in top-level workflow sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <section>: "{foo}"
    """
    When I put the caret after foo
    Then I expect no language injection
    Examples:
    | section    |
    | include    |
    | workdir    |
    | configfile |
    | report     |

  Scenario: No injection in concatenated strings
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: "{foo" + "}"
    """
    When I put the caret after foo
    Then I expect no language injection

  Scenario Outline: No injection in some sections
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      <section>: "{foo}"
    """
    When I put the caret after foo
    Then I expect no language injection
    Examples:
    | section              |
    | shadow               |
    | wildcard_constraints |
    | wrapper              |
    | version              |
    | threads              |
    | priority             |
    | singularity          |

  Scenario Outline: Inject in snakemake function calls
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    rule NAME:
      input: <function>("{foo}")
    """
    When I put the caret after foo
    Then I expect language injection on "{foo}"
    Examples:
    | function  |
    | ancient   |
    | directory |
    | temp      |
    | pipe      |
    | temporary |
    | protected |
    | dynamic   |
    | touch     |
    | repeat    |
    | report    |
    | local     |
    | expand    |
    | shell     |