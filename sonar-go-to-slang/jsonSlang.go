// SonarQube Go Plugin
// Copyright (C) 2018-2025 SonarSource SA
// mailto:info AT sonarsource DOT com
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the Sonar Source-Available License for more details.
//
// You should have received a copy of the Sonar Source-Available License
// along with this program; if not, see https://sonarsource.com/license/ssal/

package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"sort"
	"strings"
)

func toJsonSlang(node *Node, comments []*Node, tokens []*Token, errMsg *string, indent string) string {
	var buf bytes.Buffer
	buf.WriteString("{ \n")
	marshallSlangMetaData(&buf, comments, tokens, indent)
	buf.WriteString(indent + "\"tree\":\n")
	marshalIndentSlang(&buf, node, strings.Repeat(indent, 2), indent)
	buf.WriteString(indent + ",\n")
	marshalErrorMessage(&buf, errMsg, indent)
	buf.WriteString("\n} \n")
	return string(buf.Bytes())
}

func marshallSlangMetaData(dst *bytes.Buffer, comments []*Node, tokens []*Token, indent string) {
	dst.WriteString(indent + "\"treeMetaData\": {\n")
	dst.WriteString(strings.Repeat(indent, 2) + "\"comments\": [\n")

	sizeComments := len(comments)
	if sizeComments != 0 {
		for i := 0; i < sizeComments-1; i++ {
			dst.WriteString(strings.Repeat(indent, 3))
			marshallComment(dst, comments[i])
			dst.WriteString(",\n")
		}
		dst.WriteString(strings.Repeat(indent, 3))
		marshallComment(dst, comments[sizeComments-1])
		dst.WriteString("\n")
	}

	dst.WriteString(strings.Repeat(indent, 2) + "],\n")

	dst.WriteString(strings.Repeat(indent, 2) + "\"tokens\": [\n")
	sizeTokens := len(tokens)
	if sizeTokens != 0 {
		for i := 0; i < sizeTokens-1; i++ {
			marshallToken(dst, tokens[i], strings.Repeat(indent, 3))
			dst.WriteString(",\n")
		}
		marshallToken(dst, tokens[sizeTokens-1], strings.Repeat(indent, 3))
		dst.WriteString("\n")
	}
	dst.WriteString(strings.Repeat(indent, 2) + "]\n")

	dst.WriteString(indent + "},\n")
}

func marshallComment(dst *bytes.Buffer, comment *Node) {
	text := comment.Token.Value
	var contentText string

	textRange := comment.TextRange
	textContentRange := TextRange(*textRange)
	textContentRange.StartColumn = textContentRange.StartColumn + 2

	if strings.HasPrefix(text, "//") {
		contentText = text[2:]
	} else if strings.HasPrefix(text, "/*") {
		contentText = text[2 : len(text)-2]
		textContentRange.EndColumn = textContentRange.EndColumn - 2
	} else {
		panic("Unknown comment content: " + text)
	}

	dst.WriteString("{\"text\":")
	writeObjectSlang(dst, text)
	dst.WriteString(", \"contentText\":")
	writeObjectSlang(dst, contentText)
	dst.WriteString(", \"range\":")
	writeObjectSlang(dst, textRange)
	dst.WriteString(", \"contentRange\": ")
	writeObjectSlang(dst, textContentRange)
	dst.WriteString("}")
}

func marshallToken(dst *bytes.Buffer, token *Token, prefix string) {
	dst.WriteString(prefix + "{\"text\":")
	writeObjectSlang(dst, token.Value)
	dst.WriteString(",\"textRange\":")
	writeObjectSlang(dst, token.TextRange)

	if token.TokenType != other {
		dst.WriteString(",\"type\":")
		writeObjectSlang(dst, token.TokenType)
	}
	dst.WriteString("}")
}

func marshalIndentSlang(dst *bytes.Buffer, node *Node, prefix, indent string) {
	if node == nil {
		dst.WriteString(prefix + "null")
		return
	}

	dst.WriteString(prefix + "{\"@type\": ")
	writeObjectSlang(dst, node.SlangType)

	if node.TextRange != nil {
		dst.WriteString(", \"metaData\": ")
		writeObjectSlang(dst, node.TextRange)
	}

	if len(node.SlangField) != 0 {
		//Sort the fields to report them in the same order
		sortedField := sortSlangField(node.SlangField)

		for _, kv := range sortedField {
			if kv.Key == nativeKind && kv.Value == "" {
				continue
			}

			dst.WriteString(",\"" + kv.Key + "\":")

			switch obj := kv.Value.(type) {
			case *Node:
				dst.WriteString("\n")
				marshalIndentSlang(dst, obj, prefix+indent, indent)
			case []*Node:
				marshalNodeArray(dst, obj, prefix, indent)
			default:
				writeObjectSlang(dst, obj)
			}
		}
	}
	dst.WriteString("}")
}

func marshalNodeArray(dst *bytes.Buffer, nodes []*Node, prefix, indent string) {
	size := len(nodes)
	if size == 0 {
		dst.WriteString("[]")
	} else {
		dst.WriteString("[\n")
		for i := 0; i < size-1; i++ {
			marshalIndentSlang(dst, nodes[i], prefix+indent, indent)
			dst.WriteString(",\n")
		}
		marshalIndentSlang(dst, nodes[size-1], prefix+indent, indent)
		dst.WriteString("\n" + prefix + "]")
	}
}

type KeyValue struct {
	Key   string
	Value interface{}
}

func sortSlangField(slangField map[string]interface{}) []KeyValue {
	var sortedField []KeyValue
	for k, v := range slangField {
		sortedField = append(sortedField, KeyValue{k, v})
	}

	sort.Slice(sortedField, func(i, j int) bool {
		return sortedField[i].Key > sortedField[j].Key
	})

	return sortedField
}

func writeObjectSlang(dst *bytes.Buffer, obj interface{}) {
	b, err := json.Marshal(obj)
	if err != nil {
		panic(err)
	}

	dst.Write(b)
}

func marshalErrorMessage(dst *bytes.Buffer, msg *string, indent string) {
	dst.WriteString(indent + "\"error\": ")
	if msg == nil {
		dst.WriteString("null")
		return
	}
	writeObjectSlang(dst, *msg)
}

func (t TextRange) MarshalJSON() ([]byte, error) {
	if t.StartLine == t.EndLine {
		return []byte(fmt.Sprintf("\"%d:%d::%d\"", t.StartLine, t.StartColumn-1, t.EndColumn-1)), nil
	}
	return []byte(fmt.Sprintf("\"%d:%d:%d:%d\"", t.StartLine, t.StartColumn-1, t.EndLine, t.EndColumn-1)), nil
}
