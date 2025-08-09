
TEST_CASES = [
    {
        'input': "(2x+3)(4x-5)",
        'expected': "8x^2+2x-15"
    },
    {
        'input': "(-1x+2)(3x+4)",
        'expected': "-3x^2+2x+8"
    },
    {
        'input': "(2x+3)(4x+5)",
        'expected': "8x^2+22x+15"
    },
    {
        'input': "(-3+2x)(-4x+5)",
        'expected': "-8x^2+22x-15"
    },
    {
        'input': "(-2x-3)(-5-4x)",
        'expected': "8x^2+22x+15"
    },
    {
        'input': "(-3-2x)(-5-4x)",
        'expected': "8x^2+22x+15"
    },
    {
        'input': "(x+1)(x+1)",
        'expected': "x^2+2x+1"
    },
    # {
    #      # NOTE: special case
    #     'input': "(3x+0)(0x+5)",
    #     'expected': "0"
    # },
]

def expand(_str):
    pass

for case in TEST_CASES:
    result = False
    _input = case['input']
    expected = case['expected']
    try:

        actual = expand(_input)

        result = expected == actual

        print(_input, ' -> ', expected, ' == ', actual, ' = ', result)
    except Exception as e:
        print(_input, ' -> ', 'Error: ', e)
