% Scene szabinak.max
% Creator Max to Sunflow
% CreationDate 2007. 02. 09. 8:26:30
% Total Frames 101

image {
	resolution 640 480
	aa 2 2
	filter gaussian
}

trace-depths {
	diff 4
	refl 3
	refr 2
}

bucket 32 hilbert

camera {
	type pinhole
	eye 37.9861 -141.741 201.146
	target 27.8009 0.484947 10.6481
	up 0 0 1
	fov 45.0
	aspect 1.33333
}

light {
	type directional
	source 28.99 -146.813 107.955
	target -1.27492e-006 -39.2556 4.0
	radius 220.0
	emit 1.0 1.0 1.0
	intensity 1.5
}

shader {
	name Plane01_shader
	type phong
	diff 0.588235 0.588235 0.588235
	spec 0.9 0.9 0.9 100.0
}

object {
	shader Plane01_shader
	type plane
	p 4.16666 0.0 0.0
	n 0.0 0.0 1.0
}

shader {
	name Torus01_shader
	type phong
	diff 0.235294 0.92549 0.203922
	spec 0.984314 0.964706 0.47451 50.0
}

object {
	shader Torus01_shader
	transform {
		rotatey 16.6702
		translate 4.34987 34.8838 9.0
	}
	type torus
	r 9.0 32.0237
}

shader {
	name Torus02_shader
	type phong
	diff 0.235294 0.92549 0.203922
	spec 0.984314 0.964706 0.47451 50.0
}

object {
	shader Torus02_shader
	transform {
		translate 46.9424 38.5875 9.0
	}
	type torus
	r 9.0 32.0237
}

shader {
	name Torus03_shader
	type phong
	diff 0.235294 0.92549 0.203922
	spec 0.984314 0.964706 0.47451 50.0
}

object {
	shader Torus03_shader
	transform {
		translate -10.5505 -9.63422 9.0
	}
	type torus
	r 9.0 32.0237
}

shader {
	name Torus04_shader
	type phong
	diff 0.235294 0.92549 0.203922
	spec 0.984314 0.964706 0.47451 50.0
}

object {
	shader Torus04_shader
	transform {
		translate 57.0897 -34.5869 9.0
	}
	type torus
	r 9.0 32.0237
}

