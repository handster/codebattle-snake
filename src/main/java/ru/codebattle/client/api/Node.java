package ru.codebattle.client.api;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class Node {
    private BoardPoint boardPoint;
    private Node parent;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(boardPoint, node.boardPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardPoint);
    }
}
