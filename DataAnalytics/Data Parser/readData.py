import glob
from collections import Counter
from io import TextIOWrapper

import numpy as np
import plotly.graph_objects as go
import plotly.io as pio
from plotly.offline import iplot

import Consts as Con


def findFiles(path: str, name: str, ext: str):
    files = []
    for file_path in glob.glob(path + name + ext):
        files.append(open(file_path, 'r'))

    return files


def readEchoData(file: TextIOWrapper):
    lines = [line[:line.find('PSTOP') + 5] for line in file if line.startswith('PSTART')]

    # for line in lines:
    #     print(line)

    return lines


def readEchoResponseTimes(file: TextIOWrapper):
    response_times = [int(line[line.find("response_time: ") + 14: line.find(" ms")]) for line in file if
                      line.startswith(
                          'PSTART')]

    # for time in response_times:
    #     print(time)

    return response_times


def readRetransmissions(file: TextIOWrapper):
    retransmissions = [int(line[line.find("retransmissions: ") + 16:]) for line in file if
                       line.startswith(
                           'PSTART')]
    return retransmissions


def readSessionCodes(file: TextIOWrapper):
    codes = []
    for line in file:
        if line == '####\n':
            continue

        elif line == '###\n':
            break

        else:
            codes.append(line[0:5])

    # print(codes)
    return codes


def plotList(data: list):
    fig = go.FigureWidget()

    x_axis = np.arange(1, len(data) + 1)
    fig.add_trace(go.Scatter(x=x_axis, y=data, mode='lines', name='evaluation', text=data,
                             line=dict(color='#0000ff', width=2)))

    # fig.show()

    pio.kaleido.scope.default_format = "svg"
    pio.kaleido.scope.default_width = 1920
    pio.kaleido.scope.default_height = 1080
    pio.kaleido.scope.default_scale = 1

    # fig.write_image("fig.svg")
    fig.show()


def plotBarGraph(data: dict):
    x_axis = list(data.keys())
    y_axis = list(data.values())
    trace = [go.Bar(x=x_axis, y=y_axis)]

    layout = go.Layout(barmode='group', bargroupgap=0)
    fig = go.Figure(data=trace, layout=layout)

    iplot(fig)


if __name__ == '__main__':
    fs = findFiles(Con.ERR_ECHO_DATA_DIR, Con.ERR_ECHO_FILE_NAME, '.txt')

    for f in fs:
        response = readEchoResponseTimes(f)
        data_dict = Counter(response)
        print(data_dict)
        plotBarGraph(data_dict)

        # response_2 = readEchoResponseTimes(f)
        # data_dict_2 = Counter(response_2)
        # print(data_dict_2)
        # plotBarGraph(data_dict_2)
        # plotList(response)
