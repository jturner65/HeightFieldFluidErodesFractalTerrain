function [K,F] = solvePoisFE(pts, tri, ht)
% solvePoisFE solves poisson equation for passed mesh
% by calculating load function and stiffness matrix
%
% pts : list of all nodes in mesh - x,y location
% tri : list of 3-element lists of pts idx making up each point in every
% triangle
% ht : list of the heights of each element's pyramid, idxed by tri location
%

% %BUILD K MATRIX AND F VECTOR : 
% K matrix : Stiffness matrix - defines finite element function across mesh
% F vector : model eq/forcing term vector
Npts = size(pts,1);                             % number of points
Ntri = size(tri,1);                             % number of triangles
K = spalloc(Npts,Npts,3*Ntri);                  % initialize K to be sparse with room for 3*Ntri of non-zero entries - for speed, dense matrix is slower
for j = 1:Npts
    K(:,j) = [zeros(Npts-3,1)' round(rand(3,1))']';
end
F = zeros(Npts,1);                              % load vector F to hold integrals of phi's times load f(x,y)
% % USE PIECEWISE LINEAR FINITE ELEMENT METHOD TO BUILD STIFFNESS MATRIX
for elem = 1:Ntri                               % for each triangle - integrate over each triangular element
    triNds = int32(tri(elem,:));                % triNds holds 3 idx's of node locs for given element
    Pelem = [ones(3,1),pts(triNds,:)];          % point-element 3 by 3 matrix with rows=[1 nodex nodey] for each node for each point of triangle
    Aelem = abs(det(Pelem))/2;                  % area of triangle elem = half of parallelogram area <- quadrature estimate
    % linear piecewise finite element
    % *** now computer per-element Stiffness and Load coefficients
    C = inv(Pelem);                             % columns of C are coeffs for linear eq a + bx + cy to give desired basis function phi(i.e (1,0,0)) at nodes  
    grad = C(2:3,:);                            % gradient of C for b and c - slopes at each point
    Kelem = Aelem*(grad')*grad;                 % stiffness element matrix from slopes b,c in grad
    Felem = Aelem/3  * ht(elem);                % integral of phi over triangle is volume of pyramid: f(x,y)= ht(x,y) : height of fluid in sim
    K(triNds,triNds) = K(triNds,triNds) + Kelem;% add Ke to 9 entries of global K
    F(triNds) = F(triNds)+Felem;                % add Fe to 3 components of load vector F
end      