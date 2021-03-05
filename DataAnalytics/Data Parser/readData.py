import glob
import numpy as np
import plotly.graph_objects as go


fig = go.FigureWidget()

for file_path in glob.glob("./../../Networks_Assignment/Echo_Saved_Data/echo_packets *.txt"):
    file = open(file_path, 'r')

    latencies = [int(line[line.find("latency: ") + 9: line.find(" ms")]) for line in file if line.startswith("PSTART")]

    x_axis = np.arange(1, len(latencies) + 1)
    fig.add_trace(go.Scatter(x=x_axis, y=latencies, mode='lines', name='evaluation', text=latencies,
                             line=dict(color='#0000ff', width=2)))

    fig.show()
