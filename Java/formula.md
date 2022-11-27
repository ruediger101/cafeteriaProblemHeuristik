Formeln aus Paper:

$$
\begin{align}
    &\text{if } i = j \land d_{i,k} < d_{i,l} \\
    &\text{or } i < j \land d_{i,k} \leq d_{j,l} \\
    &\text{or } i < j \land d_{j,l} < d_{i,k} \leq d_{j,l} + (j - i - 1)
\end{align}
$$

Ausschreiben von Bedingung (3) liefert:

$$
\begin{align}
    &\text{if } i = j \land d_{i,k} < d_{i,l} \\
    &\text{or } i < j \land d_{i,k} \leq d_{j,l} \\
    &\text{or } i < j \land d_{i,k} > d_{j,l} \land  d_{i,k} \leq d_{j,l} + (j - i - 1)
\end{align}
$$

Invertieren von $ d_{i,k} > d_{j,l} $ in (6) ergibt:

$$
\begin{align}
    &\text{if } i = j \land d_{i,k} < d_{i,l} \\
    &\text{or } i < j \land d_{i,k} \leq d_{j,l} \\
    &\text{or } i < j \land \lnot(d_{i,k} \leq d_{j,l}) \land  d_{i,k} \leq d_{j,l} + (j - i - 1)
\end{align}
$$

Wegen $i < j$ gilt:

$$
\begin{align}
    d_{j,l} &le; d_{j,l} + (j - i - 1)
\end{align}
$$

Aus (10) folgt, dass (8) äquivalent ist zu:

$$
\begin{align}
    i < j \land d_{i,k} \leq d_{j,l}  \land  d_{i,k} \leq d_{j,l} + (j - i - 1)
\end{align}
$$

Durch Kombination von (11) mit (9) ergibt sich:

$$
\begin{align}
    i < j \land ((d_{i,k} \leq d_{j,l}) \lor \lnot(d_{i,k} \leq d_{j,l}) )\land  d_{i,k} \leq d_{j,l} + (j - i - 1)
\end{align}
$$

Was sich vereinfachen lässt zu:

$$
\begin{align}
i < j \land   d_{i,k} \leq d_{j,l} + (j - i - 1)
\end{align}
$$
    
Das heißt, dass (1), (2) & (3) äquivalent sind zu:

$$
    \begin{align}
    &\text{if } i = j \land d_{i,k} < d_{i,l} \\
    &\text{or } i < j \land   d_{i,k} \leq d_{j,l} + (j - i - 1)
    \end{align}
$$
