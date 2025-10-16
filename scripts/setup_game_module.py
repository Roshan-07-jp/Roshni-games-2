#!/usr/bin/env python3
"""
Script to set up new game modules for Roshni Games Android app.
This script automates the creation of game modules from the template.
"""

import os
import shutil
import argparse
import re
from pathlib import Path

def copy_and_replace(src, dst, replacements):
    """Copy file and replace text patterns"""
    with open(src, 'r') as f:
        content = f.read()

    for old, new in replacements.items():
        content = content.replace(old, new)

    with open(dst, 'w') as f:
        f.write(content)

def setup_game_module(game_id, game_name, category, base_path="game"):
    """Set up a new game module from template"""

    print(f"Setting up game module: {game_id}")
    print(f"Game name: {game_name}")
    print(f"Category: {category}")

    # Source template path
    template_path = Path(base_path) / "template"

    # Destination game module path
    game_path = Path(base_path) / game_id

    if game_path.exists():
        print(f"Error: Game module {game_id} already exists!")
        return False

    try:
        # Copy template directory structure
        shutil.copytree(template_path, game_path)
        print(f"✓ Created directory structure for {game_id}")

        # Update build.gradle.kts
        build_gradle_path = game_path / "build.gradle.kts"
        if build_gradle_path.exists():
            with open(build_gradle_path, 'r') as f:
                content = f.read()

            # Replace template placeholders
            content = content.replace(
                'com.roshni.games.game.GAME_ID',
                f'com.roshni.games.game.{game_id}'
            )
            content = content.replace(
                'GAME_ID',
                game_id
            )

            with open(build_gradle_path, 'w') as f:
                f.write(content)

            print(f"✓ Updated build.gradle.kts for {game_id}")

        # Update package structure
        src_path = game_path / "src/main/kotlin/com/roshni/games/game/template"
        dst_path = game_path / f"src/main/kotlin/com/roshni/games/game/{game_id}"

        if src_path.exists():
            # Rename the template directory to game_id
            os.rename(src_path, dst_path)
            print(f"✓ Updated package structure for {game_id}")

        # Update all Kotlin files with correct package names
        for kotlin_file in dst_path.rglob("*.kt"):
            with open(kotlin_file, 'r') as f:
                content = f.read()

            # Replace package declaration
            content = content.replace(
                'package com.roshni.games.game.template',
                f'package com.roshni.games.game.{game_id}'
            )

            # Replace class names and references
            content = content.replace(
                'Template',
                game_id.replace('-', '_').title().replace('_', '')
            )

            with open(kotlin_file, 'w') as f:
                f.write(content)

        print(f"✓ Updated Kotlin files for {game_id}")

        # Create game-specific configuration
        config_content = f'''package com.roshni.games.game.{game_id}

// Game configuration for {game_name}
object GameConfig {{
    const val GAME_ID = "{game_id}"
    const val GAME_NAME = "{game_name}"
    const val CATEGORY = "{category}"
    const val VERSION = "1.0.0"
    const val MIN_SDK = 24
    const val TARGET_SDK = 34
}}
'''

        config_file = dst_path / "GameConfig.kt"
        with open(config_file, 'w') as f:
            f.write(config_content)

        print(f"✓ Created game configuration for {game_id}")

        print(f"\n🎉 Successfully created game module: {game_id}")
        print("\nNext steps:")
        print(f"1. Add to settings.gradle: include(':{base_path}:{game_id}')")
        print("2. Add to app/build.gradle.kts dependencies"
        print("3. Update dynamic_feature_manifest.xml"
        print("4. Customize game logic in the created files"
        print(f"5. Add game assets to {game_path}/src/main/assets/"

        return True

    except Exception as e:
        print(f"❌ Error setting up game module: {e}")
        # Clean up partially created module
        if game_path.exists():
            shutil.rmtree(game_path)
        return False

def main():
    parser = argparse.ArgumentParser(description='Set up new game module for Roshni Games')
    parser.add_argument('--id', required=True, help='Game ID (e.g., puzzle-001)')
    parser.add_argument('--name', required=True, help='Game name (e.g., "Amazing Puzzle")')
    parser.add_argument('--category', required=True,
                       choices=['puzzle', 'word', 'arcade', 'strategy', 'casual'],
                       help='Game category')

    args = parser.parse_args()

    success = setup_game_module(args.id, args.name, args.category)

    if success:
        print("
✅ Game module setup completed successfully!"        exit(0)
    else:
        print("
❌ Game module setup failed!"        exit(1)

if __name__ == "__main__":
    main()