def first_and_last_digit_pair_sum(arr):
    return 0


TEST_CASES = [
    {
        'input': '',
        'expected': ''
    }
]

for case in TEST_CASES:
    result = False
    _input = case['input']
    _str = _input['str']
    n = _input['n']

    expected = case['expected']
    try:
        actual = caesar(_str, n, TOKEN)

        result = expected == actual

        print(_input, ' -> ', expected, ' == ', actual, ' = ', result)
    except Exception as e:
        print(_input, ' -> ', 'Error: ', e)
