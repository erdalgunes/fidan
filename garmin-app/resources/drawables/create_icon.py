#!/usr/bin/env python3
"""
Create launcher icon for Garmin app
Creates a 40x40 PNG icon with a tree design
"""

from PIL import Image, ImageDraw

# Create a 40x40 image with transparent background
img = Image.new('RGBA', (40, 40), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Background circle (green)
draw.ellipse([1, 1, 39, 39], fill=(46, 125, 50, 255), outline=(27, 94, 32, 255))

# Tree trunk (brown)
draw.rectangle([18, 24, 22, 32], fill=(109, 76, 65, 255))

# Tree crown (using triangular shapes for a pine tree look)
# Bottom triangle
draw.polygon([(9, 32), (31, 32), (20, 22)], fill=(76, 175, 80, 255))
# Middle triangle  
draw.polygon([(12, 24), (28, 24), (20, 14)], fill=(76, 175, 80, 255))
# Top triangle
draw.polygon([(15, 16), (25, 16), (20, 8)], fill=(76, 175, 80, 255))

# Add a small highlight circle
draw.ellipse([18, 13, 22, 17], fill=(129, 199, 132, 150))

# Save the PNG
img.save('launcher_icon.png', 'PNG')
print("Icon created: launcher_icon.png")