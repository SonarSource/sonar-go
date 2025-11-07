// SonarQube Go Plugin
// Copyright (C) 2018-2025 SonarSource Sàrl
// mailto:info AT sonarsource DOT com
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
	"go/ast"

	"golang.org/x/tools/go/cfg"
)

type CfgToJava struct {
	Blocks []CfgToJavaBlock
}

type CfgToJavaBlock struct {
	Node       []int32
	Successors []int32
}

func (t *SlangMapper) addNodeInCfgIdMap(node ast.Node) int32 {
	t.currentCfgId++
	t.objectToCfgIds[retrieveCfgObjectFromNode(node)] = t.currentCfgId
	return t.currentCfgId
}

// Sometimes, AST nodes are not directly mapped to the CFG blocks.
// This method is about retrieving the object that is actually used in the CFG.
// It currently maps variable declaration, from the GenDecl object to the first Spec object.
func retrieveCfgObjectFromNode(node ast.Node) any {
	if genDecl, ok := node.(*ast.GenDecl); ok {
		if len(genDecl.Specs) == 1 {
			return genDecl.Specs[0]
		}
	}
	return node
}

func (t *SlangMapper) extractCfg(body *ast.BlockStmt) *CfgToJava {
	if body == nil {
		return nil
	}
	cfgOfFunction := cfg.New(body, func(call *ast.CallExpr) bool { return true })
	cfgJavaBlocks := make([]CfgToJavaBlock, len(cfgOfFunction.Blocks))
	// For all blocks in the cfg, map them to a CfgJavaBlock
	for _, block := range cfgOfFunction.Blocks {
		cfgJavaBlock := CfgToJavaBlock{
			Node:       t.getBlockNodesIndexes(block),
			Successors: getSuccessorsIndexes(block),
		}
		cfgJavaBlocks[block.Index] = cfgJavaBlock
	}
	cfgJava := &CfgToJava{
		Blocks: cfgJavaBlocks,
	}
	return cfgJava
}

func (t *SlangMapper) reinitCfg() {
	t.currentCfgId = 0
	t.objectToCfgIds = make(map[any]int32)
}

func (t *SlangMapper) getBlockNodesIndexes(block *cfg.Block) []int32 {
	var nodesIndexes []int32
	for _, node := range block.Nodes {
		nodesIndexes = append(nodesIndexes, t.objectToCfgIds[node])
	}
	return nodesIndexes
}

func getSuccessorsIndexes(block *cfg.Block) []int32 {
	var successorsIndexes []int32
	for _, successor := range block.Succs {
		successorsIndexes = append(successorsIndexes, successor.Index)
	}
	return successorsIndexes
}
