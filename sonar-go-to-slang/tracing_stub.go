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

// The following directive is necessary to make the package coherent:
//go:build !sonartrace

package main

import (
	"context"
	"io/fs"
)

// This file is the no-op half of the tracing build-tag pair. Without the `sonartrace` tag every
// tracing entry point below compiles to an empty body, so instrumented call sites cost nothing and
// neither the tracing machinery nor its runtime/trace dependency reaches the shipped binary. The
// empty bodies are therefore deliberate; tracing.go holds the real implementations.

func initTracing() {
	// No tracing backend to open.
}

func shutdownTracing() {
	// No tracing backend to flush or close.
}

func tracingEnabled() bool { return false }

func span(ctx context.Context, name string, args ...any) (context.Context, func(...any)) {
	return ctx, func(...any) {
		// No span was started, so there is nothing to end and no event to record.
	}
}

func heapCounter(name string) {
	// No counter series to sample.
}

func flowFinish() {
	// No flow events to link.
}

func setTraceProcessLabel() {
	// No trace metadata to emit.
}

func setTraceLaneLabel(label string) {
	// No trace metadata to emit.
}

func fileSizeIfTracing(file fs.File) int64 { return 0 }
