// SonarSource Go
// Copyright (C) SonarSource Sàrl
// mailto:info AT sonarsource DOT com
//
// You can redistribute and/or modify this program under the terms of
// the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestChannelTypeIncludesDirectionAndFullRange(t *testing.T) {
	for _, channelType := range []string{"chan int", "<-chan int", "chan<- int", "chan /* direction */ <- int", "chan<- chan int", "chan <-chan int"} {
		t.Run(channelType, func(t *testing.T) {
			source := "package p\nfunc f(value " + channelType + ") {}"
			root, _, tokens, errMsg := slangFromString("channel.go", source, "ModuleNameForTest")
			require.Nil(t, errMsg)
			channel := findChannelTypeNode(root)
			require.NotNil(t, channel)
			assert.Equal(t, strings.Index(source, channelType), channel.offset)
			assert.Equal(t, channelType, source[channel.offset:channel.endOffset])
			assert.Equal(t, len("func f(value ")+1, channel.TextRange.StartColumn)
			// Every channel keyword and arrow belongs to the channel node's token range.
			var actualTokens []string
			for _, token := range tokens {
				if token.TextRange.StartLine == channel.TextRange.StartLine && token.TextRange.StartColumn >= channel.TextRange.StartColumn &&
					token.TextRange.EndColumn <= channel.TextRange.EndColumn {
					actualTokens = append(actualTokens, token.Value)
				}
			}
			require.NotEmpty(t, actualTokens)
			assert.Contains(t, actualTokens, "chan")
			if strings.Contains(channelType, "<-") {
				assert.Contains(t, actualTokens, "<-")
			}
		})
	}
}

func findChannelTypeNode(node *Node) *Node {
	if node.SlangField[nativeKind] == "Type(ChanType)" {
		return node
	}
	for _, child := range node.Children {
		if channel := findChannelTypeNode(child); channel != nil {
			return channel
		}
	}
	return nil
}
