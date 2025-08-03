def expand(str):
    return 'none'


TEST_CASES = [
    {
        'input': '',
        'expected': ''
    }
]

for case in TEST_CASES:
    try:
        result = False
        _input = case['input']
        expected = case['expected']

        actual = expand(_input)

        result = expected == actual

        print(expected, ' == ', actual, ' = ', result)
    except e:
        print('Error: ', e)
